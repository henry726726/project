from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse, JSONResponse
import uuid
import os

from qwen_module import generate_prompt_from_qwen
from nano_banana_api import generate_image_from_prompt
from render_module import insert_logo

app = FastAPI()

UPLOAD_FOLDER = "uploads"
OUTPUT_FOLDER = "outputs"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

@app.get("/")
async def root():
    return {"status": "ok", "message": "pyserver is running"}

@app.get("/healthz")
async def healthz():
    return {"status": "healthy"}

@app.post("/generate")
async def generate_image(product_name: str = Form(...), image: UploadFile = File(...)):
    try:
        # 1. 이미지 저장
        image_filename = f"{uuid.uuid4()}.png"
        image_path = os.path.join(UPLOAD_FOLDER, image_filename)
        with open(image_path, "wb") as f:
            f.write(await image.read())

        # 2. Qwen을 통해 프롬프트 생성
        prompt_json = generate_prompt_from_qwen(image_path, product_name)

        # 3. Nano Banana API에 프롬프트 전달 → 이미지 생성
        generated_image_path = generate_image_from_prompt(prompt_json)

        # 4. Render 단계에서 로고 삽입
        final_image_path = insert_logo(generated_image_path)

        # 5. 최종 이미지 반환
        return FileResponse(final_image_path, media_type="image/png")
    except Exception as e:
        return JSONResponse(status_code=500, content={"detail": f"/generate failed: {e}"})
