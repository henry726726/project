import os
import io
import json
import base64
from typing import List, Dict
from PIL import Image

from google import genai
from google.genai.types import GenerateContentConfig, Modality

class GeminiImageGenerator:
    """
    Google Gemini 2.5 Flash Image를 사용하여 배경 합성을 수행하는 클래스.
    """
    def __init__(self, model_id: str = "gemini-2.5-flash-image-preview"):
        self.model_id = model_id

        required_vars = ["GOOGLE_CLOUD_PROJECT", "GOOGLE_CLOUD_LOCATION", "GOOGLE_GENAI_USE_VERTEXAI"]
        missing = [v for v in required_vars if not os.environ.get(v)]
        if missing:
            raise EnvironmentError(f"❌ 환경변수 누락: {', '.join(missing)}")

        try:
            self.client = genai.Client()
            print(f"✅ Gemini model client for '{self.model_id}' initialized.")
        except Exception as e:
            raise RuntimeError(f"Failed to initialize Gemini client: {e}")

    def _resize_max_side(self, img: Image.Image, max_side: int) -> Image.Image:
        """최대 변을 기준으로 이미지 크기를 조절합니다."""
        w, h = img.size
        if max(w, h) <= max_side:
            return img
        if w >= h:
            nh = int(h * max_side / w)
            return img.resize((max_side, nh), Image.Resampling.LANCZOS)
        else:
            nw = int(w * max_side / h)
            return img.resize((nw, max_side), Image.Resampling.LANCZOS)

    def _build_prompt(self, meta: Dict) -> str:
        """메타데이터를 기반으로 프롬프트 문자열을 생성합니다."""
        product = meta.get("product", {})
        background = meta.get("background", {})
        layout = meta.get("layout", {})
        bg_objs = meta.get("background_objects", [])

        def get_bbox_list(items: List[Dict]) -> List[Dict]:
            return [
                {"type": i.get("type"), "bbox": i.get("bbox")}
                for i in items if isinstance(i.get("bbox"), list) and len(i["bbox"]) == 4
            ]

        negative_rects = get_bbox_list(layout.get("nongraphic_layout", []))
        negative_rects.extend(get_bbox_list(layout.get("graphic_layout", [])))

        system_text = (
            "You are an advertising image compositor. "
            "Strictly REPLACE the entire background with the requested style while preserving the product pixels. "
            "Do NOT render any text, logos, or underlay shapes. Keep all reserved boxes CLEAN. "
            "Do NOT letterbox, pad, or add borders. "
            "Absolutely DO NOT draw any letters, words, placeholder text, 'HEADLINE', 'LOGO', or similar in the reserved areas. "
            "They must remain transparent background only."
        )

        spec_lines = [
            f"- Ideal background color: {background.get('ideal_color') or 'N/A'}.",
            f"- Texture: {background.get('texture') or 'N/A'}.",
            f"- Style: {background.get('style') or 'N/A'}.",
            f"- Lighting: {background.get('lighting', {}).get('type', 'N/A')}.",
            f"- Key light direction: {background.get('lighting', {}).get('direction', 'N/A')}.",
            f"- Camera angle: {background.get('camera', {}).get('angle', 'N/A')}.",
            f"- Camera distance: {background.get('camera', {}).get('distance', 'N/A')}.",
            f"- Prefer palette: {', '.join(background.get('palette', [])) or 'N/A'}."
        ]

        if background.get("prompt"):
            spec_lines.append(f"- Positive prompt: {background['prompt']}.")
        if background.get("negative_prompt"):
            spec_lines.append(f"- Negative prompt: {background['negative_prompt']}.")

        obj_lines = [
            f"- Optional background object: {obj.get('name')}. Notes: {obj.get('notes')}"
            for obj in bg_objs
        ]

        rules = [
            "- Preserve the foreground product EXACTLY (pixel-preserve).",
            "- The original table/surface must disappear (full background replacement).",
            "- Keep all reserved boxes EMPTY (negative space).",
            "- Follow subject_layout center/ratio for framing and composition.",
            "- No vignettes, borders, or drop shadows unless explicitly asked.",
            "- Output a single photorealistic image."
        ]

        user_text = (
            "TASK: Replace the background completely while preserving the product.\n\n"
            f"PRODUCT:\n{json.dumps(product, ensure_ascii=False, indent=2)}\n\n"
            "BACKGROUND SPEC:\n" + "\n".join(filter(lambda x: "N/A" not in x, spec_lines)) + "\n\n"
            f"SUBJECT LAYOUT (normalized 0~1):\n{json.dumps(layout.get('subject_layout', {}), ensure_ascii=False, indent=2)}\n\n"
            f"RESERVED NEGATIVE SPACES (keep empty):\n{json.dumps(negative_rects, ensure_ascii=False, indent=2)}\n\n"
            "BACKGROUND OBJECTS (optional):\n" + "\n".join(obj_lines) + "\n\n"
            "RULES:\n" + "\n".join(rules)
        )
        return system_text + "\n\n" + user_text

    async def generate_image(self, image_bytes: bytes, qwen_layout: dict, product_name: str, max_side: int = 1024) -> str:
        """
        이미지 바이트와 메타데이터를 사용하여 이미지를 생성하고 바이트로 반환합니다.
        """
        try:
            # 1. 원본 이미지 리사이즈
            img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
            resized_img = self._resize_max_side(img, max_side)

            # 2. Qwen 레이아웃에서 배경 프롬프트 추출 및 이미지 생성 프롬프트 구성
            background_prompt_dict = qwen_layout.get("background", {})
            background_prompt = background_prompt_dict.get("prompt", "")

            # 3. Gemini API 페이로드 구성 (gemini-2.5-flash-image-preview 모델에 맞춤)
            payload = {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": background_prompt
                            },
                            {
                                "inlineData": {
                                    "mimeType": "image/jpeg",
                                    "data": base64.b64encode(image_bytes).decode('utf-8')
                                }
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "responseModalities": ["TEXT", "IMAGE"]
                }
            }

            # 4. Gemini API 호출
            api_key = os.getenv("GEMINI_API_KEY")
            api_url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model_id}:generateContent?key={api_key}"
            
            response = await asyncio.get_event_loop().run_in_executor(
                None,  # Use default executor
                lambda: requests.post(api_url, json=payload, timeout=600)
            )

            response.raise_for_status()
            result = response.json()
            
            # Base64 이미지 데이터 추출
            base64_data = result["predictions"][0]["bytesBase64Encoded"]
            return base64_data

        except requests.exceptions.RequestException as e:
            print(f"Gemini API 호출 실패: {e}")
            raise RuntimeError(f"Gemini API call failed: {e}")
        except Exception as e:
            print(f"Gemini 이미지 생성 실패: {e}")
            raise RuntimeError(f"Gemini image generation failed: {e}")
