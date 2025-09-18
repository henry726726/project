# nano_banana_api.py

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import FileResponse
import tempfile
import shutil

from .nano_banana_core import NanoBananaGenerator

import os

app = FastAPI()
generator = NanoBananaGenerator()


@app.post("/banana/generate/")
async def generate_banana(
    image_file: UploadFile = File(...),
    layout_json_file: UploadFile = File(...),
):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as tmp_img:
        shutil.copyfileobj(image_file.file, tmp_img)
        img_path = tmp_img.name

    with tempfile.NamedTemporaryFile(delete=False, suffix=".json", mode="w+", encoding="utf-8") as tmp_json:
        layout_content = await layout_json_file.read()
        tmp_json.write(layout_content.decode("utf-8"))
        tmp_json.flush()
        json_path = tmp_json.name

    output_path = img_path.replace(".png", "_output.png")  # 확장자 추가

    try:
        out_file = generator.generate_image(img_path, json_path, output_path)
    except Exception as e:
        # 임시파일 삭제
        os.remove(img_path)
        os.remove(json_path)
        return {"error": str(e)}

    # 처리 후 임시파일 삭제
    os.remove(img_path)
    os.remove(json_path)

    return FileResponse(out_file, media_type="image/png", filename="banana_generated.png")

'''
@app.get("/")
async def root():
    return {"message": "Nano Banana API 서버가 정상 작동 중입니다!"}
'''