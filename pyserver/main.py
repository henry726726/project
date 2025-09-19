# main.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import asyncio
from concurrent.futures import ThreadPoolExecutor
import os
import base64
import json

# 사용자 제공 qwen_module.py 파일
from qwen_module import QwenProcessor
# 이전에 리팩토링해 드린 nanobanana_module.py 파일
from nanobanana_module import GeminiImageGenerator
# 이전에 리팩토링해 드린 mysql_module.py 파일
from mysql_module import MySQLLoader

# 모델 및 DB 로더 초기화
try:
    qwen_processor = QwenProcessor()
    gemini_generator = GeminiImageGenerator()
except Exception as e:
    print(f"Failed to initialize models: {e}")
    qwen_processor = None
    gemini_generator = None

app = FastAPI()

# Pydantic 모델을 사용하여 요청 바디를 정의
class AdContentRequest(BaseModel):
    ad_content_id: int

# 데이터베이스 로더와 스레드풀 설정
mysql_loader = MySQLLoader()
executor = ThreadPoolExecutor(max_workers=5)

@app.on_event("startup")
async def startup_event():
    # 서버 시작 시 환경 변수 확인
    required_vars = ["MYSQL_HOST", "MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DB",
                     "GOOGLE_CLOUD_PROJECT", "GOOGLE_CLOUD_LOCATION", "GOOGLE_GENAI_USE_VERTEXAI"]
    if not all(os.environ.get(var) for var in required_vars):
        print("Warning: Missing one or more required environment variables.")

@app.post("/generate-final-image")
async def generate_final_image(request: AdContentRequest):
    """
    ID를 받아 MySQL에서 데이터를 가져와 Qwen과 Gemini로 순차적으로 처리하고
    결과를 DB에 저장하는 API.
    """
    if qwen_processor is None or gemini_generator is None:
        raise HTTPException(status_code=503, detail="Required AI models are not available.")

    # 1. MySQL에서 원본 데이터 로드 (원본 이미지, 제품명)
    try:
        db_data = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: mysql_loader.get_ad_content_sync(request.ad_content_id)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Database query failed: {str(e)}")

    if not db_data or not db_data.get("image_base64"):
        raise HTTPException(status_code=404, detail="Required data (image) is missing for this ID.")

    image_base64 = db_data["image_base64"]
    product_name = db_data.get("product_name") or "Product" # 제품명이 없는 경우 기본값 설정

    try:
        image_bytes = base64.b64decode(image_base64)
    except (TypeError, ValueError) as e:
        raise HTTPException(status_code=400, detail=f"Invalid Base64 string: {str(e)}")

    # 2. QwenProcessor로 이미지 분석 및 배경 프롬프트 생성 (2단계 로직 포함)
    try:
        # qwen_module.py의 process_image는 비동기 함수이므로 await로 호출
        qwen_analysis_result = await qwen_processor.process_image(image_bytes, product_name)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Qwen model processing failed: {str(e)}")
        
    # 3. Gemini 모델로 최종 이미지 생성 (Qwen의 상세 분석 결과를 그대로 사용)
    try:
        generated_image_bytes = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: gemini_generator.generate_image(image_bytes, qwen_analysis_result)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Gemini image generation failed: {str(e)}")

    generated_image_base64 = base64.b64encode(generated_image_bytes).decode('utf-8')

    # 4. 모든 분석 및 생성 결과를 JSON 객체로 통합
    final_result = {
        "qwen_analysis": qwen_analysis_result,
        "final_image_base64": generated_image_base64
    }

    # 5. 최종 결과를 MySQL RDS에 저장
    db_update_success = await asyncio.get_event_loop().run_in_executor(
        executor,
        lambda: mysql_loader.update_ad_content_result_sync(request.ad_content_id, final_result)
    )

    if not db_update_success:
        return {"status": "Processing successful, but DB update failed.", "result": final_result}

    return {"status": "All stages successful. Results saved to DB.", "result": final_result}