from fastapi import FastAPI, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from diffusers import StableDiffusionControlNetPipeline, ControlNetModel
from controlnet_aux import HEDdetector
import torch
import io
import base64

app = FastAPI()

# CORS 허용: Spring에서 호출 가능하도록 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 디바이스 설정 (GPU가 없으면 CPU fallback)
device = "cuda" if torch.cuda.is_available() else "cpu"
dtype = torch.float16 if device == "cuda" else torch.float32

# 모델 로딩
controlnet = ControlNetModel.from_pretrained(
    "lllyasviel/sd-controlnet-hed", torch_dtype=dtype
)

pipe = StableDiffusionControlNetPipeline.from_pretrained(
    "runwayml/stable-diffusion-v1-5",
    controlnet=controlnet,
    safety_checker=None,
    torch_dtype=dtype
).to(device)

# GPU일 경우에만 xformers 사용
if device == "cuda":
    pipe.enable_xformers_memory_efficient_attention()

hed = HEDdetector.from_pretrained("lllyasviel/annotators")

@app.post("/generate")
async def generate(prompt: str = Form(...), image: UploadFile = Form(...)):
    image_bytes = await image.read()
    input_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    edge_image = hed(input_image)

    result = pipe(prompt=prompt, 
                image=edge_image, 
                num_inference_steps=50,
                controlnet_conditioning_scale=0.8,   # ControlNet 영향력
                guidance_scale=8.5                   # 프롬프트 영향력 (기본 7.5~12 사이 튜닝)
    )
    output_img = result.images[0]

    buffer = io.BytesIO()
    output_img.save(buffer, format="JPEG")
    img_base64 = base64.b64encode(buffer.getvalue()).decode("utf-8")
    return {"image_base64": img_base64}
