from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
import io

# 분리해둔 모듈 import
from qwen_module import generate_json_prompt
from nano_banana_module import generate_image
from render_module import add_logo_to_image

app = FastAPI()

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
        json_prompt = generate_json_prompt(image_data, product_name)

        # 3. Nano Banana 모듈을 사용하여 이미지 생성
        generated_image_data = generate_image(json_prompt)

        # 4. Render(로고 삽입) 모듈을 사용하여 로고 추가
        final_image_data = add_logo_to_image(generated_image_data)

        # 5. 최종 이미지를 StreamingResponse로 반환
        return StreamingResponse(io.BytesIO(final_image_data), media_type="image/png")
    
    except Exception as e:
        # 오류 발생 시 HTTP 500 에러 반환
        raise HTTPException(status_code=500, detail=f"처리 중 오류가 발생했습니다: {e}")