from fastapi import FastAPI, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageDraw, ImageFont
import torch
import torchvision.transforms.functional as F
from torchvision import transforms
from torchvision.models.segmentation import deeplabv3_resnet101
import numpy as np
import cv2
import io
import base64
import os

app = FastAPI()

# CORS 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 텍스트 삽입 클래스
class TextPlacer:
    def __init__(self, device='cuda' if torch.cuda.is_available() else 'cpu'):
        self.device = device
        self.model = deeplabv3_resnet101(pretrained=True).to(self.device).eval()
        self.preprocess = transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                 std=[0.229, 0.224, 0.225]),
        ])
        self.font_path = self._find_system_font()

    def _find_system_font(self):
        if os.name == 'nt':  # Windows
            fonts = ["C:/Windows/Fonts/malgunbd.ttf", "C:/Windows/Fonts/arial.ttf"]
        elif os.uname().sysname == 'Darwin':  # macOS
            fonts = ["/Library/Fonts/Arial.ttf", "/System/Library/Fonts/Supplemental/Arial.ttf"]
        else:  # Linux
            fonts = ["/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
                     "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"]

        for path in fonts:
            if os.path.exists(path):
                return path
        return None

    def place_text_on_image(self, image_bytes: bytes, text_to_place: str, layout: str = "auto"):
        image_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")

        if layout == "box":
            return self._place_text_with_box(image_pil, text_to_place)
        elif layout == "side-box":
            return self._place_text_with_side_box(image_pil, text_to_place)
        elif layout == "expanded-side-box": # 새로운 레이아웃 추가
            return self._place_text_with_expanded_side_box(image_pil, text_to_place)

        # 기존 auto 로직 (변경 없음)
        original_width, original_height = image_pil.size
        input_tensor = self.preprocess(image_pil)
        input_batch = input_tensor.unsqueeze(0).to(self.device)

        with torch.no_grad():
            output = self.model(input_batch)['out'][0]
        segmentation_map = output.argmax(0).cpu().numpy()
        segmentation_map_resized = cv2.resize(segmentation_map.astype(np.uint8),
                                              (original_width, original_height),
                                              interpolation=cv2.INTER_NEAREST)
        bbox, center_point = self._find_largest_background_area(segmentation_map_resized)

        if bbox is None or center_point is None:
            text_x = original_width // 2
            text_y = original_height // 2
            text_color = self._get_text_color(image_pil, (0, 0, original_width, original_height))
        else:
            text_x, text_y = center_point
            text_color = self._get_text_color(image_pil, bbox)

        draw = ImageDraw.Draw(image_pil)
        font_size = int(original_width / 20)
        font = self._load_font(font_size)
        text_width, text_height = self._get_text_size(draw, text_to_place, font)

        text_x -= text_width // 2
        text_y -= text_height // 2
        text_x = max(0, min(text_x, original_width - text_width))
        text_y = max(0, min(text_y, original_height - text_height))

        draw.text((text_x, text_y), text_to_place, font=font, fill=text_color)

        buffer = io.BytesIO()
        image_pil.save(buffer, format="PNG")
        return buffer.getvalue()

    def _place_text_with_box(self, image_pil, text_to_place):
        width, height = image_pil.size
        overlay = Image.new("RGBA", image_pil.size, (0, 0, 0, 0))
        draw_overlay = ImageDraw.Draw(overlay)

        box_width = int(width * 0.5)
        box_height = int(height * 0.25)
        box_x, box_y = 40, 40
        draw_overlay.rectangle([box_x, box_y, box_x + box_width, box_y + box_height], fill=(0, 0, 0, 160))

        image_with_box = Image.alpha_composite(image_pil.convert("RGBA"), overlay)

        font = self._load_font(30)
        draw_text = ImageDraw.Draw(image_with_box)
        draw_text.text((box_x + 20, box_y + 20), text_to_place, fill="white", font=font)

        buffer = io.BytesIO()
        image_with_box.convert("RGB").save(buffer, format="PNG")
        return buffer.getvalue()

    def _place_text_with_side_box(self, image_pil, text_to_place):
        # 이 함수는 기존 이미지 위에 반투명 박스를 그리는 방식입니다.
        # "이미지 옆에 상자를 붙여서" 라는 요구사항에는 아래 expanded-side-box 가 더 적합합니다.
        width, height = image_pil.size
        box_width = int(width * 0.4)
        overlay = Image.new("RGBA", image_pil.size, (0, 0, 0, 0))
        draw_overlay = ImageDraw.Draw(overlay)

        # Draw semi-transparent right-side box
        draw_overlay.rectangle(
            [width - box_width, 0, width, height],
            fill=(0, 0, 0, 180)
        )

        # Draw text inside the box
        font = self._load_font(int(height * 0.05))
        draw_text = ImageDraw.Draw(overlay)
        margin = 20
        draw_text.text(
            (width - box_width + margin, margin),
            text_to_place,
            fill="white",
            font=font
        )

        image_with_box = Image.alpha_composite(image_pil.convert("RGBA"), overlay)

        buffer = io.BytesIO()
        image_with_box.convert("RGB").save(buffer, format="PNG")
        return buffer.getvalue()

    def _place_text_with_expanded_side_box(self, image_pil, text_to_place):
        original_width, original_height = image_pil.size
        
        # 새로운 상자 영역의 너비 (원본 이미지 너비의 약 0.5배로 설정)
        # 필요에 따라 조절 가능
        new_box_width = int(original_width * 0.5) 
        
        # 새로운 이미지의 전체 너비 계산 (원본 + 상자 너비)
        new_width = original_width + new_box_width
        new_height = original_height

        # 새로운 캔버스 생성 (새로운 배경색, 여기서는 티파니 블루와 유사한 색으로 설정)
        # RGB 값: (64, 224, 208) 또는 (0, 184, 182) 등
        # 이미지에 따라 색상 조정 필요
        new_image = Image.new("RGB", (new_width, new_height), (0, 184, 182)) # 티파니 블루 계열

        # 원본 이미지를 새 캔버스의 왼쪽에 붙여넣기
        new_image.paste(image_pil, (0, 0))

        draw = ImageDraw.Draw(new_image)
        
        # 텍스트를 삽입할 상자의 영역 (새로운 이미지의 오른쪽 부분)
        text_box_left = original_width
        text_box_top = 0
        text_box_right = new_width
        text_box_bottom = new_height
        
        # 상자 내부 여백 설정
        padding = 40 # 상자 내부에서 텍스트가 시작될 여백

        # 텍스트 줄바꿈 및 배치
        font_size = int(original_height * 0.05) # 이미지 높이에 비례하여 폰트 크기 결정
        font = self._load_font(font_size)

        # 텍스트를 줄바꿈하여 그리기 위한 함수
        def draw_wrapped_text(draw_obj, text, font_obj, text_fill_color, box_coords, line_spacing=1.2):
            x1, y1, x2, y2 = box_coords
            box_width_inner = x2 - x1 - 2 * padding
            
            words = text.split(' ')
            lines = []
            current_line = []
            
            for word in words:
                test_line = ' '.join(current_line + [word])
                # textbbox를 사용하여 정확한 텍스트 너비 계산
                left, top, right, bottom = draw_obj.textbbox((0, 0), test_line, font=font_obj)
                test_width = right - left

                if test_width <= box_width_inner:
                    current_line.append(word)
                else:
                    lines.append(' '.join(current_line))
                    current_line = [word]
            lines.append(' '.join(current_line))

            # 텍스트 전체 높이 계산
            line_height = font_obj.getbbox("Tg")[3] - font_obj.getbbox("Tg")[1] # 대략적인 한 줄 높이
            total_text_height = len(lines) * line_height * line_spacing - (line_height * (line_spacing - 1)) # 마지막 줄 간격 제외
            
            # 상자 내에서 텍스트 시작 Y 좌표 (세로 중앙 정렬)
            start_y = y1 + padding + ( (y2 - y1 - 2 * padding - total_text_height) / 2 )
            
            for line in lines:
                # 텍스트 너비 다시 계산 (줄별로)
                left, top, right, bottom = draw_obj.textbbox((0, 0), line, font=font_obj)
                line_text_width = right - left
                
                # 상자 내에서 텍스트 시작 X 좌표 (가로 중앙 정렬)
                start_x = x1 + padding + ((box_width_inner - line_text_width) / 2)
                
                draw_obj.text((start_x, start_y), line, font=font_obj, fill=text_fill_color)
                start_y += line_height * line_spacing

        # 텍스트 그리기 호출
        draw_wrapped_text(draw, text_to_place, font, "white", 
                          (text_box_left, text_box_top, text_box_right, text_box_bottom))

        buffer = io.BytesIO()
        new_image.save(buffer, format="PNG")
        return buffer.getvalue()

    def _load_font(self, size):
        if self.font_path:
            try:
                return ImageFont.truetype(self.font_path, size)
            except:
                pass
        return ImageFont.load_default()

    def _get_text_size(self, draw, text, font):
        try:
            left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
            return right - left, bottom - top
        except:
            # Deprecated for newer Pillow versions, but kept for compatibility
            return draw.textsize(text, font=font)

    def _find_largest_background_area(self, segmentation_map):
        background_mask = (segmentation_map == 0).astype(np.uint8) * 255
        if np.sum(background_mask) < (segmentation_map.size * 0.1):
            unique_classes, counts = np.unique(segmentation_map, return_counts=True)
            if len(unique_classes) > 1:
                sorted_indices = np.argsort(counts)[::-1]
                for idx in sorted_indices:
                    if unique_classes[idx] != 0:
                        background_class_id = unique_classes[idx]
                        background_mask = (segmentation_map == background_class_id).astype(np.uint8) * 255
                        break
            else:
                return None, None
        num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(background_mask, 8, cv2.CV_32S)
        if num_labels <= 1:
            return None, None
        largest_area = 0
        largest_area_label = -1
        for i in range(1, num_labels):
            area = stats[i, cv2.CC_STAT_AREA]
            if area > largest_area:
                largest_area = area
                largest_area_label = i
        if largest_area_label == -1:
            return None, None
        x = stats[largest_area_label, cv2.CC_STAT_LEFT]
        y = stats[largest_area_label, cv2.CC_STAT_TOP]
        w = stats[largest_area_label, cv2.CC_STAT_WIDTH]
        h = stats[largest_area_label, cv2.CC_STAT_HEIGHT]
        center_x, center_y = centroids[largest_area_label]
        return (int(x), int(y), int(w), int(h)), (int(center_x), int(center_y))

    def _get_text_color(self, image_pil, bbox):
        x, y, w, h = bbox
        roi = np.array(image_pil.crop((x, y, x + w, y + h)))
        if roi.size == 0:
            return (255, 255, 255)
        mean_brightness = np.mean(cv2.cvtColor(roi, cv2.COLOR_RGB2GRAY))
        return (0, 0, 0) if mean_brightness > 128 else (255, 255, 255)

# 객체 생성
placer = TextPlacer()

# API 엔드포인트
@app.post("/add_text_to_image")
async def add_text_to_image_api(
    image_file: UploadFile = Form(...),
    text: str = Form(...),
    layout: str = Form("auto")
):
    if not image_file or not text:
        raise HTTPException(status_code=400, detail="이미지와 문구는 필수입니다.")
    try:
        image_bytes = await image_file.read()
        # 수정: 새로운 레이아웃 옵션 전달
        output_image_bytes = placer.place_text_on_image(image_bytes, text, layout)
        img_base64 = base64.b64encode(output_image_bytes).decode("utf-8")
        return {"image_base64": img_base64}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"이미지 처리 오류: {e}")