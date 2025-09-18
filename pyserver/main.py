from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
import io

# 분리해둔 모듈 import
import qwen_module 
import pyserver.nano_banana_api as nano_banana_api 
import render_module 

from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse

app = FastAPI()

@app.post("/upload/")
async def upload_image(
    image: UploadFile = File(...),
    product_name: str = Form(...)
):
    # 파일 이름과 상품명 출력 (디버깅용)
    print(f"Received file: {image.filename}")
    print(f"Product name: {product_name}")

    # 이미지 처리 및 결과 생성 코드 여기에 추가
    # 예: result = generate_json_prompt(...)

    result = {"message": f"Received {image.filename} for product {product_name}"}

    return JSONResponse(content=result)


@app.post("/process-image")
async def process_image(image_file: UploadFile = File(...), product_name: str = "product"):
    """
    이미지와 제품명을 받아, Qwen을 이용해 JSON 프롬프트를 생성하고,
    Nano Banana API로 이미지를 생성한 뒤, 로고를 삽입하여 반환합니다.
    """
    try:
        # 1. 파일에서 바이트 데이터 읽기
        image_data = await image_file.read()

        # 2. Qwen 모듈을 사용하여 JSON 프롬프트 생성
        json_prompt = qwen_module(image_data, product_name)

        # 3. Nano Banana 모듈을 사용하여 이미지 생성
        generated_image_data = nano_banana_api(json_prompt)

        # 4. Render(로고 삽입) 모듈을 사용하여 로고 추가
        final_image_data = render_module(generated_image_data)

        # 5. 최종 이미지를 StreamingResponse로 반환
        return StreamingResponse(io.BytesIO(final_image_data), media_type="image/png")
    
    except Exception as e:
        # 오류 발생 시 HTTP 500 에러 반환
        raise HTTPException(status_code=500, detail=f"처리 중 오류가 발생했습니다: {e}")
