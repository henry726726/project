# nano_banana_core.py

import os
import io
import json
from PIL import Image
from typing import List, Optional

from google import genai
from google.genai.types import GenerateContentConfig, Modality


def resize_max_side(img: Image.Image, max_side: int = 1024) -> Image.Image:
    w, h = img.size
    if max(w, h) <= max_side:
        return img
    if w >= h:
        nh = int(h * max_side / w)
        return img.resize((max_side, nh), Image.LANCZOS)
    else:
        nw = int(w * max_side / h)
        return img.resize((nw, max_side), Image.LANCZOS)


class NanoBananaGenerator:
    def __init__(self, model_name="gemini-2.5-flash-image-preview", max_side=1024):
        # 환경변수 체크
        need_vars = ["GOOGLE_CLOUD_PROJECT", "GOOGLE_CLOUD_LOCATION", "GOOGLE_GENAI_USE_VERTEXAI"]
        missing = [v for v in need_vars if not os.environ.get(v)]
        if missing:
            raise EnvironmentError(f"❌ 환경변수 누락: {', '.join(missing)}")
        
        self.client = genai.Client()
        self.model = model_name
        self.max_side = max_side

    def build_prompt(self, meta: dict) -> str:
        product = meta.get("product", {})
        background = meta.get("background", {}) or {}
        layout = meta.get("layout", {}) or {}
        subj = layout.get("subject_layout", {}) or {}
        ng = layout.get("nongraphic_layout", []) or []
        gg = layout.get("graphic_layout", []) or []
        bg_objs = meta.get("background_objects", []) or []

        negative_rects = []
        for t in ng:
            b = t.get("bbox")
            if isinstance(b, list) and len(b) == 4:
                negative_rects.append({"type": t.get("type", "text"), "bbox": b})
        for g in gg:
            b = g.get("bbox")
            if isinstance(b, list) and len(b) == 4:
                negative_rects.append({"type": g.get("type", "graphic"), "bbox": b, "for": g.get("for", None)})

        spec_lines = []
        if background.get("ideal_color"):
            spec_lines.append(f"- Ideal background color: {background['ideal_color']}.")
        if background.get("texture"):
            spec_lines.append(f"- Texture: {background['texture']}.")
        if background.get("style"):
            spec_lines.append(f"- Style: {background['style']}.")
        if background.get("lighting", {}).get("type"):
            spec_lines.append(f"- Lighting: {background['lighting']['type']}.")
        if background.get("lighting", {}).get("direction"):
            spec_lines.append(f"- Key light direction: {background['lighting']['direction']}.")
        if background.get("camera", {}).get("angle"):
            spec_lines.append(f"- Camera angle: {background['camera']['angle']}.")
        if background.get("camera", {}).get("distance"):
            spec_lines.append(f"- Camera distance: {background['camera']['distance']}.")
        if background.get("palette"):
            spec_lines.append(f"- Prefer palette: {', '.join(background['palette'])}.")
        if background.get("background_prompt"):
            spec_lines.append(f"- Positive prompt: {background['background_prompt']}")
        if background.get("negative_prompt"):
            spec_lines.append(f"- Negative prompt: {background['negative_prompt']}")

        obj_lines = []
        for obj in bg_objs:
            line = f"- Optional background object: {obj.get('name')}"
            if obj.get("style"): line += f" (style: {obj['style']})"
            if obj.get("depth"): line += f", depth: {obj['depth']}"
            if obj.get("bbox_hint"): line += f", place within bbox_hint {obj['bbox_hint']}"
            if obj.get("notes"): line += f", notes: {obj['notes']}"
            obj_lines.append(line + ".")

        system_text = (
            "You are an advertising image compositor.\n"
            "Strictly REPLACE the entire background with the requested style while preserving the product pixels.\n"
            "Do NOT render any text, logos, or underlay shapes. Keep all reserved boxes CLEAN.\n"
            "Do NOT letterbox, pad, or add borders."
        )

        rules = [
            "- Preserve the foreground product EXACTLY (pixel-preserve).",
            "- The original table/surface must disappear.",
            "- Keep all reserved boxes EMPTY (negative space).",
            "- Follow subject_layout center/ratio.",
            "- No vignettes, borders, or drop shadows.",
            "- Output a single photorealistic image."
        ]

        user_text = (
            f"TASK: Replace the background completely while preserving the product.\n\n"
            f"PRODUCT:\n{json.dumps(product, ensure_ascii=False)}\n\n"
            f"BACKGROUND SPEC:\n" + "\n".join(spec_lines) + "\n\n"
            f"SUBJECT LAYOUT:\n{json.dumps({'subject_layout': subj}, ensure_ascii=False)}\n\n"
            f"RESERVED NEGATIVE SPACES:\n{json.dumps(negative_rects, ensure_ascii=False)}\n\n"
            f"BACKGROUND OBJECTS:\n" + ("\n".join(obj_lines) if obj_lines else "(none)") + "\n\n"
            f"RULES:\n" + "\n".join(rules)
        )

        return system_text + "\n\n" + user_text

    def generate_image(self, image_path: str, layout_json_path: str, output_path: str) -> str:
        with open(layout_json_path, "r", encoding="utf-8") as f:
            meta = json.load(f)

        img = Image.open(image_path).convert("RGB")
        img = resize_max_side(img, self.max_side)

        buf = io.BytesIO()
        img.save(buf, format="PNG")
        image_bytes = buf.getvalue()

        prompt_text = self.build_prompt(meta)

        cfg = GenerateContentConfig(
            response_modalities=[Modality.TEXT, Modality.IMAGE],
            candidate_count=1,
        )

        response = self.client.models.generate_content(
            model=self.model,
            contents=[prompt_text, img],
            config=cfg,
        )

        cand = response.candidates[0] if response.candidates else None
        if not cand or not getattr(cand, "content", None):
            raise ValueError("⚠️ No valid response from model.")

        parts = getattr(cand.content, "parts", []) or []
        for p in parts:
            if getattr(p, "inline_data", None):
                mime = p.inline_data.mime_type
                data = p.inline_data.data
                ext = mime.split("/")[-1].replace("jpeg", "jpg")
                out_file = f"{os.path.splitext(output_path)[0]}.{ext}"
                with open(out_file, "wb") as f:
                    f.write(data)
                return out_file

        raise ValueError("⚠️ No image part received in the response.")
