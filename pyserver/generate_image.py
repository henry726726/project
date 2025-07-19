from fastapi import FastAPI, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
# StableDiffusionXLControlNetImg2ImgPipeline 임포트
from diffusers import StableDiffusionXLControlNetImg2ImgPipeline, ControlNetModel 
import torch
import io
import base64
import os # 폰트 경로 지정을 위해 필요할 수 있습니다.

# Canny 엣지 검출을 위한 라이브러리 임포트
from controlnet_aux import CannyDetector

app = FastAPI()

# --- CORS 설정 ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # 개발 중에는 모든 출처 허용. 배포 시에는 특정 도메인으로 제한 권장.
    allow_methods=["*"], # 모든 HTTP 메서드 허용
    allow_headers=["*"], # 모든 헤더 허용
)

# --- 디바이스 및 데이터 타입 설정 ---
device = "cuda" if torch.cuda.is_available() else "cpu"
# SDXL은 일반적으로 float16을 사용하여 메모리를 절약하고 속도를 높입니다.
# CPU 환경에서는 float32를 사용해야 합니다.
dtype = torch.float16 if device == "cuda" else torch.float32

# --- Canny 엣지 검출기 초기화 ---
# 이 검출기는 텍스트 마스크 이미지를 Canny 엣지 맵으로 변환하는 데 사용됩니다.
canny_detector = CannyDetector()
print("✅ Canny 엣지 검출기 초기화 완료.")

# --- 모델 로딩 ---
# 2. SDXL 호환 ControlNet Canny 모델 로드 - 파이프라인보다 먼저 로드
# 텍스트 마스크(글자 윤곽선)를 인식하는 데 적합한 Canny ControlNet을 사용합니다.
try:
    # SDXL용 Canny ControlNet 모델 (이전과 동일한 'stabilityai/control-lora-canny-sdxl-1.0' 사용)
    controlnet_text_adapter = ControlNetModel.from_pretrained(
        "stabilityai/control-lora-canny-sdxl-1.0", 
        torch_dtype=dtype
    )
    print("✅ ControlNet Canny 모델 (SDXL용) 로드 완료.")
except Exception as e:
    print(f"❌ ControlNet Canny 모델 로드 실패: {e}. 정확한 모델 이름을 확인해주세요!")
    exit()

# 1. Stable Diffusion XL 기본 모델 및 ControlNetImg2ImgPipeline 로드
# 이제 ControlNet을 Image-to-Image 파이프라인과 함께 사용합니다.
try:
    pipe = StableDiffusionXLControlNetImg2ImgPipeline.from_pretrained(
        "stabilityai/stable-diffusion-xl-base-1.0", # SDXL 기본 모델
        controlnet=controlnet_text_adapter, # ControlNet 모델을 파이프라인에 직접 전달
        torch_dtype=dtype,
        variant="fp16" if dtype == torch.float16 else None # fp16 variant 사용 (메모리 절약)
    )
    print("✅ Stable Diffusion XL ControlNet Img2Img 파이프라인 로드 완료.")
except Exception as e:
    print(f"❌ Stable Diffusion XL ControlNet Img2Img 파이pe라인 로드 실패: {e}")
    # 모델 로드 실패 시 애플리케이션 종료 또는 적절한 오류 처리
    exit()

pipe = pipe.to(device)

# GPU일 경우에만 xformers 사용 (SDXL에서도 권장)
if device == "cuda":
    try:
        pipe.enable_xformers_memory_efficient_attention()
        print("✅ xformers 메모리 효율적 어텐션 활성화.")
    except Exception as e:
        print(f"⚠️ xformers 활성화 실패 (CPU 또는 xformers 미설치): {e}")

# --- API 엔드포인트: 이미지 생성 ---
@app.post("/generate")
async def generate(
    prompt: str = Form(...),
    negative_prompt: str = Form(None), # 부정적인 프롬프트 추가
    # 사용자가 업로드한 기본 이미지 (JPG/PNG 형태)
    base_image_file: UploadFile = Form(...),
    # ControlNet을 위한 프론트엔드에서 생성된 텍스트 마스크 이미지 파일 (흑백 글자 이미지)
    text_mask_file: UploadFile = Form(...),
    controlnet_conditioning_scale: float = Form(0.8), # ControlNet 영향력 파라미터화
    guidance_scale: float = Form(8.5), # 프롬프트 영향력 파라미터화
    num_inference_steps: int = Form(50), # 스텝 수 파라미터화
    # Img2Img 강도: 원본 이미지를 얼마나 변경할지 (0.0~1.0, 1.0에 가까울수록 원본 변화 큼)
    strength: float = Form(0.8)
):
    if not prompt:
        raise HTTPException(status_code=400, detail="프롬프트가 필요합니다.")
    if not base_image_file:
        raise HTTPException(status_code=400, detail="합성할 기본 이미지가 필요합니다.")
    if not text_mask_file:
        raise HTTPException(status_code=400, detail="ControlNet 조건화를 위한 텍스트 마스크 이미지가 필요합니다.")

    try:
        # 1. 기본 이미지 처리 (사용자가 업로드한 JPG/PNG)
        base_image_bytes = await base_image_file.read()
        input_base_image = Image.open(io.BytesIO(base_image_bytes)).convert("RGB")
        # SDXL 기본 해상도에 맞도록 크기 조정
        input_base_image = input_base_image.resize((1024, 1024))
        print(f"🖼️ 입력된 기본 이미지 크기: {input_base_image.size}")


        # 2. ControlNet 텍스트 마스크 이미지 처리 (프론트엔드에서 생성)
        text_mask_image_bytes = await text_mask_file.read()
        input_text_mask_image = Image.open(io.BytesIO(text_mask_image_bytes)).convert("RGB")
        # ControlNet도 일관된 크기를 기대하므로 1024x1024로 조정
        input_text_mask_image = input_text_mask_image.resize((1024, 1024))
        print(f"🎨 입력된 ControlNet 텍스트 마스크 이미지 크기: {input_text_mask_image.size}")

        # 3. 텍스트 마스크 이미지를 Canny 엣지 맵으로 변환
        # CannyDetector를 사용하여 텍스트 마스크 이미지에서 엣지를 추출합니다.
        # low_threshold와 high_threshold는 필요에 따라 조절할 수 있습니다.
        canny_image = canny_detector(input_text_mask_image, low_threshold=100, high_threshold=200)
        print("✨ 텍스트 마스크 이미지를 Canny 엣지 맵으로 변환 완료.")


        # 이미지 생성 파이프라인 호출 (ControlNet Img2Img)
        # 'image' 파라미터는 Img2Img의 시작 이미지를, 'control_image'는 ControlNet의 조건 이미지를 받습니다.
        result = pipe(
            prompt=prompt,
            negative_prompt=negative_prompt, # 부정적인 프롬프트 적용
            image=input_base_image,          # 사용자가 업로드한 JPG 이미지가 이 이미지로 들어갑니다.
            control_image=canny_image,       # 이제 Canny 엣지 맵이 ControlNet의 조건으로 사용됩니다.
            num_inference_steps=num_inference_steps,
            controlnet_conditioning_scale=controlnet_conditioning_scale,
            guidance_scale=guidance_scale,
            strength=strength # Img2Img 강도 적용
        )
        output_img = result.images[0]
        print("✅ 이미지 합성 완료.")

        # 생성된 이미지를 Base64로 인코딩하여 반환
        buffer = io.BytesIO()
        output_img.save(buffer, format="JPEG") # JPEG 또는 PNG (투명도 필요시)
        img_base64 = base64.b64encode(buffer.getvalue()).decode("utf-8")

        return {"image_base64": img_base64}

    except Exception as e:
        print(f"❌ 이미지 생성 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=f"이미지 생성 중 서버 오류: {e}")
