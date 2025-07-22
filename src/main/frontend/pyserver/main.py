from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageDraw, ImageFont
import io
import base64

app = FastAPI()

origins = ["http://localhost:3000", "http://localhost:8080"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.post("/generate")
async def generate_image(image: UploadFile = File(...), prompt: str = Form(...)):
    try:
        image_bytes = await image.read()
        input_image = Image.open(io.BytesIO(image_bytes)).convert("RGBA")
        draw = ImageDraw.Draw(input_image)

        try:
            font_path = "C:/Users/msi/project/src/main/resources/NanumGothic.ttf"  # 한글 폰트 경로
            font = ImageFont.truetype(font_path, 40)
        except IOError:
            font = ImageFont.load_default()
            print("경고: 한글 폰트를 찾을 수 없어 기본 폰트를 사용합니다.")

        text = prompt
        position = (10, 10)

        bbox = draw.textbbox(position, text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]

        background_padding = 10
        rect_x1 = position[0]
        rect_y1 = position[1]
        rect_x2 = position[0] + text_width + background_padding
        rect_y2 = position[1] + text_height + background_padding

        draw.rectangle([rect_x1, rect_y1, rect_x2, rect_y2], fill=(255, 255, 255, 128))
        draw.text((position[0] + background_padding/2, position[1] + background_padding/2), text, font=font, fill=(0, 0, 0, 255))

        buffered = io.BytesIO()
        input_image.save(buffered, format="PNG")
        img_str = base64.b64encode(buffered.getvalue()).decode()

        return JSONResponse(content={"image_base64": img_str, "message": "이미지 합성 성공"})
    except Exception as e:
        return JSONResponse(content={"image_base64": "", "message": f"이미지 합성 실패: {e}"}, status_code=500)