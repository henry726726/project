from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import json, argparse, os, sys, re, io
import torch
from transformers import Qwen2_5_VLForConditionalGeneration, AutoProcessor
from qwen_vl_utils import process_vision_info
from PIL import Image

import torch
#from some_model_lib import Qwen2_5_VLForConditionalGeneration  # 가상의 import, 실제 모델 경로에 맞게 변경하세요

import uvicorn

app = FastAPI()

# -------------------------------------------------
# 1) First-pass schema: product/background summary + layout JSON
# -------------------------------------------------
SCHEMA_TEXT = (
    '오직 다음 JSON만 출력해. 다른 설명/마크다운 금지.\n'
    '{\n'
    '  "product": { "type":"...", "material":"...", "design":"...", "features":"..." },\n'
    '  "background": { "ideal_color":"...", "texture":"...", "lighting":"...", "style":"..." },\n'
    '  "layout": {\n'
    '    "subject_layout": { "center":[cx,cy], "ratio":[rw,rh] },\n'
    '    "nongraphic_layout": [\n'
    '      {"type":"headline","bbox":[x,y,w,h],"confidence":c},\n'
    '      {"type":"subhead","bbox":[x,y,w,h],"confidence":c}\n'
    '    ],\n'
    '    "graphic_layout": [\n'
    '      {"type":"logo","content":"","bbox":[x,y,w,h],"confidence":c}\n'
    '    ]\n'
    '  }\n'
    '}\n'
    '\n'
    '# 제약 사항:\n'
    '- 모든 좌표/크기는 0~1 비율. bbox=[x,y,w,h].\n'
    '- subject와 텍스트의 IoU < 0.2 권장.\n'
    '- 텍스트끼리 겹침을 줄여(권장 IoU < 0.3). 겹치면 confidence 낮은 쪽.\n'
    '- 텍스트 박스는 가로형을 우선 추천(가로:세로 비 1.8 이상 권장).\n'
    '- headline 1+, logo 1+ 최소 추천(확신이 낮으면 confidence만 낮게).\n'
    '- 불확실하더라도 최소 후보는 제시.\n'
)

SYSTEM = (
    "너는 광고 상품 이미지를 분석해 제품·배경 요약과 레이아웃을 JSON으로 제안하는 VLM이야. "
    "이미지 맥락만으로 텍스트/로고 위치를 추천하고, confidence로 확신도를 제공해. "
    "JSON 외의 텍스트는 출력하지 마."
)

# -------------------------------------------------
# 2) Second-pass: Background prompt planner (EN, long & detailed)
# -------------------------------------------------
BG_SYSTEM = (
    "You design prompts for a background generator (e.g., SDXL/Flux/Imagen). "
    "Respect the subject/text/logo boxes and free-space; never occlude them. "
    "Return JSON only. "
    "Write the `background_prompt` in NATURAL ENGLISH, 120–200 words, 8–12 sentences, no bullets. "
    "Describe environment, surface, depth, palette harmony, composition, lens/camera angle, lighting quality & direction, and prop placement that avoids overlaps. "
    "Include concrete cues for materials, textures, and mood."
)

BG_SCHEMA = (
    'Only output the following JSON. No explanations.\n'
    '{\n'
    '  "background_prompt": "... (120-200 words, natural English, no bullets)",\n'
    '  "negative_prompt": "... (things to avoid; artifacts, occlusions, off-palette colors)",\n'
    '  "camera": { "angle":"eye-level|top-down|low-angle|macro|oblique", "distance":"closeup|medium|wide" },\n'
    '  "lighting": { "type":"soft|hard|rim|ambient", "direction":"left|right|front|back|top|bottom" },\n'
    '  "palette": ["#RRGGBB", "..."],\n'
    '  "objects": [\n'
    '    { "name":"flower petals", "style":"bokeh|flat|painterly|realistic", "bbox_hint":[x,y,w,h],\n'
    '      "depth":"behind_product|same_plane", "avoid_iou_with":"subject,text_boxes,logo_boxes",\n'
    '      "notes":"subject와 텍스트 박스 IoU<0.1; avoid overlaps" }\n'
    '  ]\n'
    '}\n'
)

# -------------------------------------------------
# Utilities: JSON parsing & geometry helpers
# -------------------------------------------------

def extract_json(text: str):
    try:
        start = text.index("{")
        end = text.rindex("}") + 1
        return json.loads(text[start:end])
    except Exception:
        return {"raw": text}


def clip01(v: float) -> float:
    return max(0.0, min(1.0, float(v)))


def clip_bbox(b):
    x, y, w, h = b
    x = clip01(x); y = clip01(y)
    w = clip01(w); h = clip01(h)
    if x + w > 1: w = max(0.0, 1 - x)
    if y + h > 1: h = max(0.0, 1 - y)
    return [x, y, w, h]


def iou(b1, b2):
    x1, y1, w1, h1 = b1; x2, y2, w2, h2 = b2
    xa = max(x1, x2); ya = max(y1, y2)
    xb = min(x1 + w1, x2 + w2); yb = min(y1 + h1, y2 + h2)
    inter = max(0.0, xb - xa) * max(0.0, yb - ya)
    a1 = max(0.0, w1 * h1); a2 = max(0.0, w2 * h2)
    union = a1 + a2 - inter
    return inter / union if union > 0 else 0.0


def nms(boxes, iou_thr=0.3):
    bxs = [b for b in boxes if isinstance(b.get("bbox"), list) and len(b["bbox"]) == 4]
    bxs.sort(key=lambda b: float(b.get("confidence", 0.5)), reverse=True)
    kept = []
    for b in bxs:
        if all(iou(b["bbox"], k["bbox"]) < iou_thr for k in kept):
            kept.append(b)
    return kept


def enforce_text_rules(items, subject_bbox, min_ar=1.8, min_margin=0.03, max_iou=0.2):
    out = []
    for it in items:
        b = it.get("bbox", [0, 0, 0, 0])
        if not (isinstance(b, list) and len(b) == 4):
            continue
        b = clip_bbox(b)
        if subject_bbox and iou(b, subject_bbox) >= max_iou:
            continue
        x, y, w, h = b
        if x < min_margin or y < min_margin or x + w > 1 - min_margin or y + h > 1 - min_margin:
            continue
        ar = (w / h) if h > 0 else 999
        if ar < min_ar:
            continue
        it["confidence"] = float(it.get("confidence", 0.5))
        it["bbox"] = b
        out.append(it)
    return out


def postprocess_layout(parsed, text_iou_thr=0.3, logo_iou_thr=0.3, subj_text_iou_max=0.2):
    if not isinstance(parsed, dict) or "layout" not in parsed:
        return parsed
    layout = parsed["layout"]
    subj = layout.get("subject_layout", {})
    try:
        cx, cy = subj.get("center", [0.5, 0.5])
        rw, rh = subj.get("ratio", [0.3, 0.3])
        subject_bbox = clip_bbox([cx - rw / 2, cy - rh / 2, rw, rh])
    except Exception:
        subject_bbox = [0.4, 0.4, 0.2, 0.2]
    texts = layout.get("nongraphic_layout", [])
    if not isinstance(texts, list):
        texts = []
    texts = [{**t, "confidence": float(t.get("confidence", 0.5))} for t in texts]
    texts = enforce_text_rules(texts, subject_bbox, max_iou=subj_text_iou_max)
    texts = nms(texts, iou_thr=text_iou_thr)
    for i, t in enumerate(texts):
        t.setdefault("id", f"text#{i}")
    graphics = layout.get("graphic_layout", [])
    if not isinstance(graphics, list):
        graphics = []
    cleaned_g = []
    for g in graphics:
        b = g.get("bbox")
        if not (isinstance(b, list) and len(b) == 4):
            continue
        b = clip_bbox(b)
        if iou(b, subject_bbox) > 0.4:
            continue
        g["bbox"] = b
        g["confidence"] = float(g.get("confidence", 0.5))
        cleaned_g.append(g)
    graphics = nms(cleaned_g, iou_thr=logo_iou_thr)
    layout["nongraphic_layout"] = texts
    layout["graphic_layout"] = graphics
    return parsed

# -------------------------------------------------
# Normalization: pixel -> 0~1
# -------------------------------------------------

def normalize_if_pixels_layout(parsed, image_path):
    try:
        W, H = Image.open(image_path).size
    except Exception:
        W, H = 1, 1

    def norm_center_ratio(center, ratio):
        cx, cy = center; rw, rh = ratio
        if max(cx, cy) > 1.0:
            cx = float(cx) / W
            cy = float(cy) / H
        if max(rw, rh) > 1.0:
            rw = float(rw) / W
            rh = float(rh) / H
        return [clip01(cx), clip01(cy)], [clip01(rw), clip01(rh)]

    def norm_bbox(b):
        x, y, w, h = b
        if max(x, y, w, h) > 1.0:
            x = float(x) / W
            y = float(y) / H
            w = float(w) / W
            h = float(h) / H
        return clip_bbox([x, y, w, h])

    if not isinstance(parsed, dict) or "layout" not in parsed:
        return parsed

    layout = parsed["layout"]
    subj = layout.get("subject_layout", {})
    c = subj.get("center", [0.5, 0.5])
    r = subj.get("ratio", [0.3, 0.3])
    c, r = norm_center_ratio(c, r)
    layout["subject_layout"] = {"center": c, "ratio": r}

    if isinstance(layout.get("nongraphic_layout"), list):
        for t in layout["nongraphic_layout"]:
            if "bbox" in t and isinstance(t["bbox"], list) and len(t["bbox"]) == 4:
                t["bbox"] = norm_bbox(t["bbox"])

    if isinstance(layout.get("graphic_layout"), list):
        for g in layout["graphic_layout"]:
            if "bbox" in g and isinstance(g["bbox"], list) and len(g["bbox"]) == 4:
                g["bbox"] = norm_bbox(g["bbox"])

    return parsed

# -------------------------------------------------
# Fallbacks: inject default text banners & logo
# -------------------------------------------------

def inject_fallback_boxes(parsed, headline_h=0.12, margin=0.04, logo_box=(0.25, 0.10)):
    if "layout" not in parsed or not isinstance(parsed["layout"], dict):
        parsed["layout"] = {}
    layout = parsed["layout"]

    s = layout.get("subject_layout", {"center": [0.5, 0.5], "ratio": [0.3, 0.3]})
    cx, cy = s.get("center", [0.5, 0.5]); rw, rh = s.get("ratio", [0.3, 0.3])
    subj = clip_bbox([cx - rw / 2, cy - rh / 2, rw, rh])

    ng = layout.get("nongraphic_layout")
    if not isinstance(ng, list) or len(ng) == 0:
        top = [margin, margin, 1 - 2 * margin, headline_h]
        bot = [margin, 1 - margin - headline_h, 1 - 2 * margin, headline_h]
        def shrink_if_overlap(b):
            if iou(b, subj) >= 0.1:
                x, y, w, h = b
                return [x, y, w, max(0.05, h * 0.5)]
            return b
        layout["nongraphic_layout"] = [
            {"type": "headline", "bbox": clip_bbox(shrink_if_overlap(top)), "confidence": 0.5},
            {"type": "headline", "bbox": clip_bbox(shrink_if_overlap(bot)), "confidence": 0.5},
        ]

    gg = layout.get("graphic_layout")
    if not isinstance(gg, list) or len(gg) == 0:
        lw, lh = logo_box
        gx = 1 - margin - lw; gy = margin
        logo = clip_bbox([gx, gy, lw, lh])
        if iou(logo, subj) >= 0.3:
            logo = clip_bbox([margin, margin, lw, lh])
        layout["graphic_layout"] = [
            {"type": "logo", "content": "", "bbox": logo, "confidence": 0.5}
        ]

    return parsed

# -------------------------------------------------
# Text underlays for readability
# -------------------------------------------------

def add_text_underlays(parsed, pad=0.015, opacity=0.6, radius=0.08):
    if not isinstance(parsed, dict) or "layout" not in parsed:
        return parsed
    layout = parsed["layout"]
    texts = layout.get("nongraphic_layout", []) or []
    if not isinstance(layout.get("graphic_layout"), list):
        layout["graphic_layout"] = []

    def expand(b, p):
        x, y, w, h = b
        return clip_bbox([x - p, y - p, w + 2 * p, h + 2 * p])

    for idx, t in enumerate(texts):
        b = t.get("bbox")
        if not (isinstance(b, list) and len(b) == 4):
            continue
        under = {
            "type": "underlay",
            "for": t.get("type", "text") + f"#{idx}",
            "bbox": expand(b, pad),
            "style": {"shape": "rounded", "radius": radius, "opacity": opacity},
            "confidence": min(0.9, float(t.get("confidence", 0.5)) + 0.1),
        }
        layout["graphic_layout"].append(under)
    return parsed

# -------------------------------------------------
# Palette extraction (PIL adaptive)
# -------------------------------------------------

def extract_palette_hex(image_path, k=5):
    try:
        im = Image.open(image_path).convert("RGB")
        im_thumb = im.copy(); im_thumb.thumbnail((256, 256))
        pal = im_thumb.convert("P", palette=Image.ADAPTIVE, colors=k).convert("RGB")
        colors = pal.getcolors(256 * 256) or []
        colors.sort(key=lambda x: x[0], reverse=True)
        hexes = []
        for _, rgb in colors[:k]:
            hexes.append('#%02x%02x%02x' % rgb)
        dedup = []
        for h in hexes:
            if h not in dedup:
                dedup.append(h)
        return dedup[:k] or ["#ffffff", "#000000"]
    except Exception:
        return ["#ffffff", "#000000"]

# -------------------------------------------------
# Main class wrapping original functions
# -------------------------------------------------

class GenerateJSON:
    def __init__(self, model_id="Qwen/Qwen2.5-VL-7B-Instruct"):
        self.model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
            model_id, dtype="auto", device_map="auto"
        )
        self.processor = AutoProcessor.from_pretrained(model_id, use_fast=False)

    # --- layout summary for background context
    def summarize_layout_for_bg(self, parsed):
        if not isinstance(parsed, dict) or "layout" not in parsed:
            return "no layout"
        layout = parsed["layout"]
        subj = layout.get("subject_layout", {"center": [0.5, 0.5], "ratio": [0.3, 0.3]})
        cx, cy = subj.get("center", [0.5, 0.5])
        rw, rh = subj.get("ratio", [0.3, 0.3])
        subj_bbox = [cx - rw / 2, cy - rh / 2, rw, rh]

        texts = layout.get("nongraphic_layout", []) or []
        logos = layout.get("graphic_layout", []) or []

        top_free = cy - rh / 2
        bottom_free = 1 - (cy + rh / 2)
        left_free = cx - rw / 2
        right_free = 1 - (cx + rw / 2)
        free_hints = []
        if top_free > 0.25: free_hints.append("top has ample negative space")
        if bottom_free > 0.25: free_hints.append("bottom has ample negative space")
        if left_free > 0.25: free_hints.append("left side has ample negative space")
        if right_free > 0.25: free_hints.append("right side has ample negative space")

        return json.dumps({
            "subject_bbox": subj_bbox,
            "text_boxes": [t.get("bbox") for t in texts if isinstance(t.get("bbox"), list)],
            "logo_boxes": [g.get("bbox") for g in logos if isinstance(g.get("bbox"), list)],
            "free_space_hints": free_hints
        }, ensure_ascii=False)

    # --- extract background fields from JSON-like text
    def extract_bg_fields_from_text(self, gen_text: str):
        def _unescape(s):
            try:
                return bytes(s, 'utf-8').decode('unicode_escape')
            except Exception:
                return s

        out = {"background_prompt": None, "negative_prompt": None, "camera": {}, "lighting": {}}

        m = re.search(r'"background_prompt"\s*:\s*"(.+?)"', gen_text, flags=re.S)
        if m:
            out["background_prompt"] = _unescape(m.group(1)).strip()

        m = re.search(r'"negative_prompt"\s*:\s*"(.+?)"', gen_text, flags=re.S)
        if m:
            out["negative_prompt"] = _unescape(m.group(1)).strip()

        m = re.search(r'"camera"\s*:\s*\{[^}]*"angle"\s*:\s*"([^"]+)"', gen_text, flags=re.S)
        if m:
            out["camera"]["angle"] = m.group(1).strip()
        m = re.search(r'"camera"\s*:\s*\{[^}]*"distance"\s*:\s*"([^"]+)"', gen_text, flags=re.S)
        if m:
            out["camera"]["distance"] = m.group(1).strip()

        m = re.search(r'"lighting"\s*:\s*\{[^}]*"type"\s*:\s*"([^"]+)"', gen_text, flags=re.S)
        if m:
            out["lighting"]["type"] = m.group(1).strip()
        m = re.search(r'"lighting"\s*:\s*\{[^}]*"direction"\s*:\s*"([^"]+)"', gen_text, flags=re.S)
        if m:
            out["lighting"]["direction"] = m.group(1).strip()

        return out

    # --- build default negative prompt
    def _build_negative_prompt_default(self):
        return (
            "busy patterns, harsh shadows, specular clipping on metal, occluding props, "
            "over-saturated colors outside palette, tilted horizon, extreme perspective distortion, "
            "text overlays, watermarks, low resolution, compression artifacts, banding, "
            "motion blur, depth-of-field that hides the subject, human figures, hands"
        )

    # --- ensure background prompts
    def ensure_background_prompts(self, parsed, product_name, context_json, min_chars=800):
        bg = parsed.setdefault("background", {})
        prompt_raw = (bg.get("prompt") or '').strip()
        negative = (bg.get("negative_prompt") or '').strip()

        # If prompt looks like JSON blob, extract fields
        if prompt_raw.startswith('{') or '"background_prompt"' in prompt_raw:
            fields = self.extract_bg_fields_from_text(prompt_raw)
            if fields.get("background_prompt"):
                bg["prompt"] = fields["background_prompt"]
            if fields.get("negative_prompt") and not negative:
                negative = fields["negative_prompt"]
            cam = bg.get("camera") or {}
            cam.update(fields.get("camera") or {})
            bg["camera"] = cam
            light = bg.get("lighting") or {}
            light.update(fields.get("lighting") or {})
            bg["lighting"] = light

        prompt = (bg.get("prompt") or '').strip()

        # Normalize lighting/camera types; handle Korean/strings
        light_raw = bg.get("lighting", {})
        if isinstance(light_raw, dict):
            light = light_raw
        elif isinstance(light_raw, str) and light_raw.strip():
            light = {"type": light_raw.strip()}
        else:
            light = {}

        kor2eng_light = {
            "균일한 밝기": "soft", "부드러운": "soft", "딱딱한": "hard", "림": "rim", "주변광": "ambient",
            "상단": "top", "하단": "bottom", "앞": "front", "뒤": "back", "왼쪽": "left", "오른쪽": "right",
        }
        if "type" in light:
            light["type"] = kor2eng_light.get(light["type"], light["type"]) or "soft"
        if "direction" in light:
            light["direction"] = kor2eng_light.get(light["direction"], light["direction"]) or "front"

        cam_raw = bg.get("camera", {})
        cam = cam_raw if isinstance(cam_raw, dict) else {}

        angle = str(cam.get("angle", "eye-level")).replace('_', '-')
        distance = str(cam.get("distance", "closeup"))
        ltype = (light.get("type") or "soft").lower()
        ldir = (light.get("direction") or "front").lower()
        palette = bg.get("palette") or []

        # Enforce long prose if too short
        if len(prompt) < min_chars:
            free_hints = []
            try:
                ctx = json.loads(context_json) if isinstance(context_json, str) else (context_json or {})
                free_hints = ctx.get("free_space_hints", []) or []
            except Exception:
                pass
            free_txt = f" The composition preserves ample {', '.join(free_hints)} for copy layers." if free_hints else ""
            palette_txt = f" The color harmony follows {', '.join(palette)}." if palette else ""
            prompt = (
                f"{prompt} This shot is a {angle} camera angle, {distance} distance, "
                f"lit by {ltype} lighting from {ldir}.{free_txt}{palette_txt}"
            )

            if not negative:
                negative = self._build_negative_prompt_default()

        bg["prompt"] = prompt
        bg["negative_prompt"] = negative
        bg["lighting"] = light
        bg["camera"] = cam
        bg["palette"] = palette

        return parsed

    # --- generate first-pass JSON summary from image and text prompt
    def generate_first_pass_json(self, img, prompt):
        inputs = self.processor(images=img, text=prompt, return_tensors="pt").to(self.model.device)
        outputs = self.model.generate(**inputs, max_new_tokens=1024)
        gen_text = self.processor.batch_decode(outputs, skip_special_tokens=True)[0]
        parsed = extract_json(gen_text)
        return parsed

    # --- generate background prompt (second pass)
    def generate_background_prompt(self, img, context_json):
        inputs = self.processor(images=img, text=BG_SYSTEM + BG_SCHEMA + context_json, return_tensors="pt").to(self.model.device)
        outputs = self.model.generate(**inputs, max_new_tokens=1024)
        gen_text = self.processor.batch_decode(outputs, skip_special_tokens=True)[0]
        parsed = extract_json(gen_text)
        return parsed


generator = GenerateJSON()  # 모델 및 프로세서 초기화 (한번만)

@app.post("/generate_json/")
async def generate_json_endpoint(
    product_name: str = Form(...),
    image_file: UploadFile = File(...)
):
    try:
        # 이미지 파일 로드
        img_bytes = await image_file.read()
        img = Image.open(io.BytesIO(img_bytes)).convert("RGB")

        # 1) first-pass prompt (SCHEMA_TEXT + 제품명 힌트)
        prompt = f"[제품명 힌트] {product_name}\n{SCHEMA_TEXT}"

        # 2) 첫 번째 pass JSON 생성
        parsed = generator.generate_first_pass_json(img, prompt)

        # 3) 레이아웃 요약 생성
        context_json = generator.summarize_layout_for_bg(parsed)

        # 4) 배경 프롬프트 생성
        bg_plan = generator.generate_background_prompt(img, context_json)

        # 5) 배경 필드 보강
        enriched = generator.ensure_background_prompts(parsed, product_name, context_json)

        return JSONResponse(content=enriched)

    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})

if __name__ == "__main__":
    # 서버 실행
    uvicorn.run("qwen_module:app", host="0.0.0.0", port=8000, reload=True)