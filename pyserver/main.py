# main.py (수정 버전 - 폰트 크기 계산 로직 개선)

from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageDraw, ImageFont
import io
import base64
import textwrap
import os
import traceback

app = FastAPI()

origins = ["http://localhost:3000", "http://localhost:8080"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 💡💡💡 get_fitting_font 함수 수정: 여러 줄 텍스트의 가장 긴 줄 길이를 기준으로 폰트 크기 계산 💡💡💡
def get_fitting_font(draw, text, max_width, font_path, max_font_size=40, min_font_size=10):
    font_size = max_font_size
    lines = text.split('\n') # 여러 줄 텍스트를 줄 단위로 분리
    longest_line = ""
    if lines:
        longest_line = max(lines, key=len) # 가장 긴 줄 찾기 (문자 수 기준)
        # 만약 픽셀 기준으로 가장 긴 줄을 찾고 싶다면, 아래 while 루프를 응용하여 각 줄을 테스트해야 함

    font = ImageFont.truetype(font_path, font_size)

    # 💡💡💡 draw.textlength 대신 가장 긴 줄의 길이를 측정하여 폰트 크기 조정 💡💡💡
    while font_size > min_font_size and draw.textlength(longest_line, font=font) > max_width:
        font_size -= 1
        font = ImageFont.truetype(font_path, font_size)
    return font

@app.post("/generate")
async def generate_image(image: UploadFile = File(...), prompt: str = Form(...)):
    try:
        image_bytes = await image.read()
        input_image = Image.open(io.BytesIO(image_bytes)).convert("RGBA")
        draw = ImageDraw.Draw(input_image)

        font_path = "C:/Users/msi/project/src/main/resources/fonts/NanumGothic.ttf" # 💡💡💡 폰트 경로 다시 확인. pyserver 폴더 내에 폰트가 있는지.
        # 만약 폰트 파일이 project/src/main/resources에 있다면,
        # font_path = "C:/Users/msi/project/src/main/resources/NanumGothic.ttf"
        # 또는 상대경로로: os.path.join(os.path.dirname(__file__), '../src/main/resources/NanumGothic.ttf')

        if not os.path.exists(font_path):
            print(f"ERROR: Font file not found at path: {font_path}")
            return JSONResponse(content={"image_base64": "", "message": f"폰트 파일을 찾을 수 없습니다. 경로 확인: {font_path}"}, status_code=500)

        max_width = input_image.width - 20
        wrapped_text = textwrap.fill(prompt, width=30)

        font = get_fitting_font(draw, wrapped_text, max_width, font_path) # 수정된 함수 호출
        
        x, y = 10, 10

        lines = wrapped_text.split('\n')
        
        # 💡💡💡 줄 높이 계산 개선 (getbbox 또는 font.getmetrics 사용) 💡💡💡
        if lines:
            # 첫 번째 줄을 기준으로 높이를 계산합니다. (폰트의 전체 높이)
            # getbbox('A')[3] - getbbox('A')[1]은 텍스트의 실제 내용 높이만 측정합니다.
            # getmetrics()[0] + getmetrics()[1]는 폰트의 상승선(ascent)과 하강선(descent) 합입니다.
            # font.getsize(lines[0])[1]는 텍스트의 전체 높이를 반환합니다 (버전 Deprecated 유의)
            # 가장 안정적인 방법: ascent + descent
            ascent, descent = font.getmetrics()
            line_height = ascent + descent + 5 # 폰트 자체의 줄 높이 + 패딩
        else:
            line_height = 0

        # 텍스트 박스 너비 계산 (각 줄의 너비를 측정하여 가장 넓은 값 선택)
        rect_width = 0
        for line in lines:
            line_text_width = draw.textlength(line, font=font)
            if line_text_width > rect_width:
                rect_width = line_text_width
        rect_width += 10 # 좌우 패딩

        rect_height = line_height * len(lines) + 10 # 상하 패딩

        # 텍스트 박스가 이미지 밖으로 나가는 경우 처리 (필요시 추가)
        if x + rect_width > input_image.width or y + rect_height > input_image.height:
            print(f"WARNING: Text box (w:{rect_width}, h:{rect_height}) goes beyond image boundaries (w:{input_image.width}, h:{input_image.height}). Some text might be clipped.")
            # 여기서 에러를 발생시키거나, 텍스트를 줄이거나, 이미지 크기를 조정하는 등의 처리 필요.
            # 지금은 경고만 출력하고 계속 진행.

        draw.rectangle([x - 5, y - 5, x + rect_width, y + rect_height], fill=(255, 255, 255, 128))

        for line in lines:
            draw.text((x, y), line, font=font, fill=(0, 0, 0, 255))
            y += line_height

        buffered = io.BytesIO()
        input_image.save(buffered, format="PNG")
        img_str = base64.b64encode(buffered.getvalue()).decode()

        return JSONResponse(content={"image_base64": img_str, "message": "이미지 합성 성공"})
    except Exception as e:
        print(f"ERROR: Image generation failed: {e}")
        traceback.print_exc()
        return JSONResponse(content={"image_base64": "", "message": f"이미지 합성 실패: {e}. 상세 에러는 서버 로그를 확인해주세요."}, status_code=500)