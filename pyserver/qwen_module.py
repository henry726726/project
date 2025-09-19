import os
import io
import json
import base64
import re
import torch
import argparse
from PIL import Image
from typing import List, Dict, Union

from transformers import AutoProcessor
from modelscope import snapshot_download
from modelscope.models.multi_modal import Qwen2_5_VLForConditionalGeneration

# 필요한 외부 함수들 (미리 정의되어 있다고 가정)
from utils.qwen_utils import (
    process_vision_info, extract_json, normalize_if_pixels_layout,
    postprocess_layout, inject_fallback_boxes, add_text_underlays,
    SYSTEM, SCHEMA_TEXT, BG_SCHEMA, BG_SYSTEM
)

class BackgroundPlanner:
    def __init__(self, model_id="Qwen/Qwen2.5-VL-7B-Instruct"):
        print("[모델 로딩 중...]")
        self.model = Qwen2_5_VLForConditionalGeneration.from_pretrained(model_id, device_map="auto", dtype="auto")
        self.processor = AutoProcessor.from_pretrained(model_id, use_fast=False)

    def extract_palette_hex(self, image: Image.Image, k=5) -> List[str]:
        try:
            im_thumb = image.copy(); im_thumb.thumbnail((256, 256))
            pal = im_thumb.convert("P", palette=Image.ADAPTIVE, colors=k).convert("RGB")
            colors = pal.getcolors(256 * 256) or []
            colors.sort(key=lambda x: x[0], reverse=True)
            hexes = ['#%02x%02x%02x' % rgb for _, rgb in colors[:k]]
            dedup = list(dict.fromkeys(hexes))
            return dedup[:k] or ["#ffffff", "#000000"]
        except Exception:
            return ["#ffffff", "#000000"]

    def summarize_layout_for_bg(self, parsed: Dict) -> str:
        layout = parsed.get("layout", {})
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

    def build_negative_prompt_default(self) -> str:
        return (
            "busy patterns, harsh shadows, specular clipping on metal, occluding props, "
            "over-saturated colors outside palette, tilted horizon, extreme perspective distortion, "
            "text overlays, watermarks, low resolution, compression artifacts, banding, "
            "motion blur, depth-of-field that hides the subject, human figures, hands"
        )

    def process(self, image_base64: str, product_name: str) -> Dict:
        image = Image.open(io.BytesIO(base64.b64decode(image_base64.split(",")[-1]))).convert("RGB")
        palette = self.extract_palette_hex(image, k=5)

        # 1차 추론
        user_text = f"[제품명 힌트] {product_name}\n{SCHEMA_TEXT}"
        messages = [
            {"role": "system", "content": [{"type": "text", "text": SYSTEM}]},
            {"role": "user", "content": [
                {"type": "image", "image": image},
                {"type": "text", "text": user_text}
            ]}
        ]
        text = self.processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        image_inputs, video_inputs = process_vision_info(messages)
        inputs = self.processor(text=[text], images=image_inputs, videos=video_inputs,
                                padding=True, return_tensors="pt").to(self.model.device)

        with torch.no_grad():
            out_ids = self.model.generate(
                **inputs,
                max_new_tokens=900,
                do_sample=True,
                top_p=0.9,
                temperature=0.7
            )

        gen_text = self.processor.batch_decode(out_ids[:, inputs.input_ids.shape[1]:], skip_special_tokens=True)[0]

        # 2차 처리
        parsed = extract_json(gen_text)
        parsed = normalize_if_pixels_layout(parsed, image)
        parsed = postprocess_layout(parsed)
        parsed = inject_fallback_boxes(parsed)
        parsed = add_text_underlays(parsed)

        # 배경 정보 생성
        context_for_bg = self.summarize_layout_for_bg(parsed)
        parsed = ensure_background_prompts(parsed, product_name, context_for_bg, min_chars=800)

        return parsed
