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

# --- CORS 설정 ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 텍스트 배치 로직 클래스 ---
class TextPlacer:
    def __init__(self, device='cuda' if torch.cuda.is_available() else 'cpu'):
        self.device = device
        self.model = deeplabv3_resnet101(pretrained=True).to(self.device).eval()
        self.preprocess = transforms.Compose([
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ])
        print(f"✅ DeepLabV3+ 모델이 {self.device}에 로드되었습니다.")

        self.font_path = self._find_system_font()
        if self.font_path:
            print(f"✅ 폰트 경로 발견: {self.font_path}")
        else:
            print("⚠️ 시스템 폰트를 찾을 수 없습니다. 기본 폰트가 사용될 수 있습니다.")

    def _find_system_font(self):
        if os.name == 'nt':
            font_candidates = [
                "C:/Windows/Fonts/malgunbd.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/NanumGothic.ttf"
            ]
        elif os.uname().sysname == 'Darwin':
            font_candidates = [
                "/Library/Fonts/Arial.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "/System/Library/Fonts/AppleSDGothicNeo.ttc"
            ]
        else:
            font_candidates = [
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
                "/usr/share/fonts/truetype/arial.ttf"
            ]

        for path in font_candidates:
            if os.path.exists(path):
                return path
        return None

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
                        print(f"⚠️ 0번 배경 클래스가 작아, 가장 큰 다른 클래스({background_class_id})를 배경으로 간주합니다.")
                        break
            else:
                print("⚠️ 적합한 배경 영역을 찾을 수 없습니다. 기본값 처리.")
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
        if w == 0 or h == 0:
            return (255, 255, 255)
            
        roi = np.array(image_pil.crop((x, y, x + w, y + h)))
        
        if roi.size == 0:
            return (255, 255, 255)
        
        mean_brightness = np.mean(cv2.cvtColor(roi, cv2.COLOR_RGB2GRAY))

        if mean_brightness > 128:
            return (0, 0, 0)
        else:
            return (255, 255, 255)

    def place_text_on_image(self, image_bytes: bytes, text_to_place: str):
        """
        바이트 형태의 이미지와 문구를 받아 처리하고, 결과를 바이트 형태로 반환합니다.
        """
        print("DEBUG: [1/7] 이미지 바이트 로드 및 PIL 이미지 변환 시작")
        image_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        original_width, original_height = image_pil.size
        print(f"DEBUG: [1/7] 이미지 로드 및 크기 확인: {original_width}x{original_height}")

        print("DEBUG: [2/7] 이미지 전처리 (ToTensor, Normalize) 시작")
        input_tensor = self.preprocess(image_pil)
        input_batch = input_tensor.unsqueeze(0).to(self.device)
        print("DEBUG: [2/7] 이미지 전처리 완료")

        print("DEBUG: [3/7] 모델 추론 (Semantic Segmentation) 시작")
        with torch.no_grad():
            output = self.model(input_batch)['out'][0]
        print("DEBUG: [3/7] 모델 추론 완료")
        
        print("DEBUG: [4/7] 세그멘테이션 맵 생성 및 리사이즈 시작")
        segmentation_map = output.argmax(0).cpu().numpy()
        segmentation_map_resized = cv2.resize(segmentation_map.astype(np.uint8), 
                                              (original_width, original_height), 
                                              interpolation=cv2.INTER_NEAREST)
        print("DEBUG: [4/7] 세그멘테이션 맵 생성 및 리사이즈 완료")

        print("DEBUG: [5/7] 가장 큰 배경 영역 및 텍스트 색상 결정 시작")
        bbox, center_point = self._find_largest_background_area(segmentation_map_resized)

        if bbox is None or center_point is None:
            print("⚠️ 적합한 배경 영역을 찾을 수 없습니다. 이미지 중앙에 텍스트를 배치합니다.")
            text_x = original_width // 2
            text_y = original_height // 2
            text_color = self._get_text_color(image_pil, (0, 0, original_width, original_height)) 
        else:
            text_x, text_y = center_point
            text_color = self._get_text_color(image_pil, bbox)
        print("DEBUG: [5/7] 배경 영역 및 텍스트 색상 결정 완료")

        print("DEBUG: [6/7] 텍스트 렌더링 준비 및 위치 계산 시작")
        draw = ImageDraw.Draw(image_pil)

        font_size = int(original_width / 20) 
        font = None
        if self.font_path:
            try:
                font = ImageFont.truetype(self.font_path, font_size)
            except IOError:
                print(f"⚠️ 지정된 폰트 경로 '{self.font_path}'에서 폰트를 로드할 수 없습니다. 기본 폰트를 사용합니다.")
        
        if font is None:
            font = ImageFont.load_default()
            font_size = 20 

        try:
            left, top, right, bottom = draw.textbbox((0, 0), text_to_place, font=font)
            text_width = right - left
            text_height = bottom - top
        except AttributeError:
            text_width, text_height = draw.textsize(text_to_place, font=font)

        text_x -= text_width // 2
        text_y -= text_height // 2

        text_x = max(0, min(text_x, original_width - text_width))
        text_y = max(0, min(text_y, original_height - text_height))
        print("DEBUG: [6/7] 텍스트 렌더링 위치 계산 완료")

        print("DEBUG: [7/7] 이미지에 텍스트 그리기 및 바이트 변환 시작")
        draw.text((text_x, text_y), text_to_place, font=font, fill=text_color)

        buffer = io.BytesIO()
        image_pil.save(buffer, format="PNG")
        print("DEBUG: [7/7] 이미지에 텍스트 그리기 및 바이트 변환 완료")
        
        return buffer.getvalue()

placer = TextPlacer()

@app.post("/add_text_to_image")
async def add_text_to_image_api(
    image_file: UploadFile = Form(...),
    text: str = Form(...)
):
    if not image_file:
        raise HTTPException(status_code=400, detail="이미지 파일이 필요합니다.")
    if not text:
        raise HTTPException(status_code=400, detail="문구가 필요합니다.")

    try:
        print("DEBUG: API 요청 수신. 이미지 파일 읽기 시작.")
        image_bytes = await image_file.read()
        print("DEBUG: 이미지 파일 읽기 완료. TextPlacer 호출.")
        
        output_image_bytes = placer.place_text_on_image(image_bytes, text)
        
        print("DEBUG: TextPlacer 처리 완료. Base64 인코딩 시작.")
        img_base64 = base64.b64encode(output_image_bytes).decode("utf-8")
        print("DEBUG: Base64 인코딩 완료. 응답 반환.")
        
        return {"image_base64": img_base64}
    except Exception as e:
        print(f"❌ 이미지 처리 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=f"이미지 처리 중 서버 오류: {e}")