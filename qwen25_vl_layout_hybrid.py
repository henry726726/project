import json, argparse, os, sys, re
import torch
from transformers import Qwen2_5_VLForConditionalGeneration, AutoProcessor
from qwen_vl_utils import process_vision_info
from PIL import Image

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
    '    "nongraphic_layout": [ {"type":"headline","bbox":[x,y,w,h],"confidence":c} ],\n'
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
    '- 불확실하면 confidence를 낮게 주되, 각 1개씩은 반드시 제안.\n'
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

def pick_single_text(texts, subject_bbox):
    if not texts:
        return []
    cx = subject_bbox[0] + subject_bbox[2]/2
    cy = subject_bbox[1] + subject_bbox[3]/2
    top_free    = subject_bbox[1]
    bottom_free = 1 - (subject_bbox[1] + subject_bbox[3])
    left_free   = subject_bbox[0]
    right_free  = 1 - (subject_bbox[0] + subject_bbox[2])
    prefer = max(
        [('top', top_free), ('bottom', bottom_free), ('left', left_free), ('right', right_free)],
        key=lambda kv: kv[1]
    )[0]

    def center(b): return (b[0]+b[2]/2, b[1]+b[3]/2)
    def score(t):
        b = t['bbox']; x, y = center(b)
        iou_pen = iou(b, subject_bbox)
        ar = (b[2]/b[3]) if b[3] > 0 else 9
        area = b[2]*b[3]
        align = (
            (prefer=='top' and y < cy) or
            (prefer=='bottom' and y > cy) or
            (prefer=='left' and x < cx) or
            (prefer=='right' and x > cx)
        )
        align_bonus = 0.2 if align else 0.0
        return (t.get('confidence',0.5)) + align_bonus + 0.1*min(ar/3,1.0) + 0.05*area - 0.6*iou_pen
    return [max(texts, key=score)]

def pick_single_logo(graphics, subject_bbox, chosen_text):
    logos = [g for g in graphics if g.get("type") == "logo"]
    if not logos:
        return []
    tb = chosen_text[0]['bbox'] if chosen_text else None
    def score(g):
        b = g['bbox']
        pen = 0.5*iou(b, subject_bbox) + (0.4*iou(b, tb) if tb else 0.0)
        area = b[2]*b[3]
        return g.get('confidence',0.5) + 0.05*area - pen
    return [max(logos, key=score)]


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
    texts = pick_single_text(texts, subject_bbox)
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
    graphics = pick_single_logo(graphics, subject_bbox, texts)
    
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

        top_free    = subj[1]
        bottom_free = 1 - (subj[1] + subj[3])
        pick = top if top_free >= bottom_free else bot

        if iou(pick, subj) >= 0.1:
            x, y, w, h = pick
            pick = [x, y, w, max(0.05, h * 0.5)]
        layout["nongraphic_layout"] = [
            {"type": "headline", "bbox": clip_bbox(pick), "confidence": 0.5}
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
# Layout summary for background context
# -------------------------------------------------

def summarize_layout_for_bg(parsed):
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

# -------------------------------------------------
# Background prompt helpers: defaults and robust extraction
# -------------------------------------------------

def _build_negative_prompt_default():
    return (
        "busy patterns, harsh shadows, specular clipping on metal, occluding props, "
        "over-saturated colors outside palette, tilted horizon, extreme perspective distortion, "
        "text overlays, watermarks, low resolution, compression artifacts, banding, "
        "motion blur, depth-of-field that hides the subject, human figures, hands"
    )


def extract_bg_fields_from_text(gen_text: str):
    """When 2nd-pass JSON is truncated/embedded as string, pull useful fields via regex."""
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


def ensure_background_prompts(parsed, product_name, context_json, min_chars=800):
    """Ensure long natural-English prompt & safe negative; normalize camera/lighting.
       Accepts string/dict light/camera; extracts fields if prompt contains JSON string.
    """
    bg = parsed.setdefault("background", {})
    prompt_raw = (bg.get("prompt") or '').strip()
    negative = (bg.get("negative_prompt") or '').strip()

    # If prompt looks like a JSON blob, extract fields from it
    if prompt_raw.startswith('{') or '"background_prompt"' in prompt_raw:
        fields = extract_bg_fields_from_text(prompt_raw)
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
        product_txt = f" The scene flatters the {product_name}." if product_name else " The scene flatters the product."
        prompt = (
            f"A minimal studio scene viewed {angle} at a {distance} distance, built on a clean white base with a "
            f"uniform, smooth surface. The background feels airy and uncluttered so the jewelry remains the visual anchor. "
            f"Lighting is {ltype} from the {ldir}, creating gentle highlights on metal and controlled shadows that avoid the subject. "
            f"Subtle gradients and a clean vignette guide the eye toward the center without stealing attention. "
            f"Props are placed behind the product and outside text/logo areas; their scale is modest and their edges soft to prevent occlusion. "
            f"Depth cues are introduced with a mild blur in the far plane and a crisp focus on the subject plane. "
            f"Micro-texture is barely perceptible, keeping reflections smooth and elegant. "
            f"Overall mood is calm, premium, and editorial." + palette_txt + free_txt + product_txt
        )
        bg["prompt"] = prompt

    if not negative:
        bg["negative_prompt"] = _build_negative_prompt_default()
    else:
        bg["negative_prompt"] = negative

    cam["angle"] = angle
    cam["distance"] = distance
    light["type"] = ltype
    light["direction"] = ldir
    bg["camera"] = cam
    bg["lighting"] = light
    parsed["background"] = bg
    return parsed

# -------------------------------------------------
# Second-pass call to VLM for background planning
# -------------------------------------------------

def generate_bg_plan(model, processor, image_path, product_name, parsed, palette,
                     max_new_tokens=1000, top_p=0.9, temperature=0.7):
    context = summarize_layout_for_bg(parsed)
    user_text = (
        f"[제품명 힌트] {product_name or ''}\n"
        f"[레이아웃 컨텍스트] {context}\n"
        f"[권장 팔레트] {palette}\n"
        f"{BG_SCHEMA}"
    )
    messages = [
        {"role": "system", "content": [{"type": "text", "text": BG_SYSTEM}]},
        {"role": "user", "content": [
            {"type": "image", "image": f"file://{image_path}"},
            {"type": "text", "text": user_text}
        ]}
    ]
    text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    image_inputs, video_inputs = process_vision_info(messages)
    inputs = processor(text=[text], images=image_inputs, videos=video_inputs,
                       padding=True, return_tensors="pt").to(model.device)

    with torch.no_grad():
        out_ids = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            do_sample=True,
            top_p=top_p,
            temperature=temperature
        )

    gen = processor.batch_decode(out_ids[:, inputs.input_ids.shape[1]:], skip_special_tokens=True)[0]
    try:
        start = gen.index('{'); end = gen.rindex('}') + 1
        return json.loads(gen[start:end])
    except Exception:
        fields = extract_bg_fields_from_text(gen)
        return {
            "background_prompt": fields.get("background_prompt") or gen.strip()[:1200],
            "negative_prompt": fields.get("negative_prompt") or "",
            "camera": fields.get("camera") or {},
            "lighting": fields.get("lighting") or {},
            "palette": palette,
            "objects": []
        }

# -------------------------------------------------
# Main
# -------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--image", help="분석할 이미지 경로", required=False)
    ap.add_argument("--product_name", help="제품 이름(힌트)", default=None)
    ap.add_argument("--max_new_tokens", type=int, default=900)
    ap.add_argument("--temperature", type=float, default=0.7)
    ap.add_argument("--top_p", type=float, default=0.9)
    ap.add_argument("--bg_min_chars", type=int, default=900, help="배경 프롬프트 최소 길이(문자 수)")
    ap.add_argument("--save", help="결과를 저장할 파일 경로(json)", default=None)
    ap.add_argument("--bg_prompt", action="store_true", help="배경 프롬프트/소품 계획 생성 활성화")
    args = ap.parse_args()

    product_name = args.product_name or input("제품 이름을 입력하세요: ").strip()
    image_path = args.image or input("제품 이미지 파일 경로를 입력하세요 (예: './image.jpg'): ").strip()
    if not os.path.exists(image_path):
        print(f"[에러] 이미지 경로를 찾을 수 없습니다: {image_path}", file=sys.stderr)
        sys.exit(1)

    model_id = "Qwen/Qwen2.5-VL-7B-Instruct"
    model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
        model_id, dtype="auto", device_map="auto"
    )
    processor = AutoProcessor.from_pretrained(model_id, use_fast=False)

    user_text = f"[제품명 힌트] {product_name}\n{SCHEMA_TEXT}"
    messages = [
        {"role": "system", "content": [{"type": "text", "text": SYSTEM}]},
        {"role": "user", "content": [
            {"type": "image", "image": f"file://{image_path}"},
            {"type": "text", "text": user_text}
        ]}
    ]

    text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    image_inputs, video_inputs = process_vision_info(messages)
    inputs = processor(text=[text], images=image_inputs, videos=video_inputs,
                       padding=True, return_tensors="pt").to(model.device)

    with torch.no_grad():
        out_ids = model.generate(
            **inputs,
            max_new_tokens=args.max_new_tokens,
            do_sample=True,
            top_p=args.top_p,
            temperature=args.temperature
        )

    gen_text = processor.batch_decode(
        out_ids[:, inputs.input_ids.shape[1]:],
        skip_special_tokens=True
    )[0]

    # First-pass: parse & refine
    parsed = extract_json(gen_text)
    parsed = normalize_if_pixels_layout(parsed, image_path)
    parsed = postprocess_layout(parsed)
    parsed = inject_fallback_boxes(parsed)
    parsed = add_text_underlays(parsed)

    # Second-pass: background prompt & objects
    need_bg = (args.bg_prompt or not (parsed.get("background", {}).get("prompt")))
    if need_bg:
        palette = extract_palette_hex(image_path, k=5)
        bg_plan = generate_bg_plan(
            model, processor, image_path, product_name, parsed, palette,
            max_new_tokens=max(1000, args.max_new_tokens),
            top_p=args.top_p, temperature=args.temperature
        )
        if "background" not in parsed or not isinstance(parsed["background"], dict):
            parsed["background"] = {}
        parsed["background"].update({
            "prompt": bg_plan.get("background_prompt", ""),
            "negative_prompt": bg_plan.get("negative_prompt", ""),
            "camera": bg_plan.get("camera", parsed.get("background", {}).get("camera", {})),
            "lighting": bg_plan.get("lighting", parsed.get("background", {}).get("lighting", {})),
            "palette": bg_plan.get("palette", palette)
        })
        parsed.setdefault("background_objects", bg_plan.get("objects", parsed.get("background_objects", [])))

    # Ensure long/filled prompts regardless of 2nd-pass outcome
    context_for_bg = summarize_layout_for_bg(parsed)
    parsed = ensure_background_prompts(parsed, product_name, context_for_bg, min_chars=args.bg_min_chars)

    if args.save:
        with open(args.save, "w", encoding="utf-8") as f:
            json.dump(parsed, f, ensure_ascii=False, indent=2)
        print(f"[저장 완료] {args.save}")
    else:
        print(json.dumps(parsed, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
