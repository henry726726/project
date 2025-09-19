# main.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import asyncio
from concurrent.futures import ThreadPoolExecutor
import os
import base64
import json
import io
import pymysql

# 기존 모듈 임포트
from mysql_module import MySQLLoader
from qwen_module import QwenProcessor
from nanobanana_module import GeminiImageGenerator

# 새로 추가된 텍스트 렌더링 모듈 임포트
from ad_text_render_module import AdTextRenderer

# 모델 및 DB 로더 초기화
try:
    qwen_processor = QwenProcessor()
    gemini_generator = GeminiImageGenerator()
    ad_text_renderer = AdTextRenderer() # 폰트 경로는 필요에 따라 수정
except Exception as e:
    print(f"Failed to initialize modules: {e}")
    qwen_processor = None
    gemini_generator = None
    ad_text_renderer = None

app = FastAPI()

# Pydantic 모델을 사용하여 요청 바디를 정의
class AdContentRequest(BaseModel):
    ad_content_id: int

# 데이터베이스 로더와 스레드풀 설정
mysql_loader = MySQLLoader()
executor = ThreadPoolExecutor(max_workers=5)

@app.on_event("startup")
async def startup_event():
    required_vars = ["MYSQL_HOST", "MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DB",
                     "GOOGLE_CLOUD_PROJECT", "GOOGLE_CLOUD_LOCATION", "GOOGLE_GENAI_USE_VERTEXAI"]
    if not all(os.environ.get(var) for var in required_vars):
        print("Warning: Missing one or more required environment variables.")

@app.post("/generate-ad-with-text")
async def generate_ad_with_text(request: AdContentRequest):
    """
    ID를 받아 DB에서 모든 정보를 로드하고, Qwen, Gemini, Text Renderer를
    순차적으로 실행하여 최종 광고 이미지를 생성하고 DB에 저장하는 API.
    """
    if not all([qwen_processor, gemini_generator, ad_text_renderer]):
        raise HTTPException(status_code=503, detail="Required modules are not available.")

    # 1. MySQL에서 데이터 로드 (원본 이미지, 제품명, Qwen/Gemini 결과, 로고 이미지 등)
    try:
        db_data = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: mysql_loader.get_ad_content_for_rendering(request.ad_content_id)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Database query failed: {str(e)}")

    if not db_data:
        raise HTTPException(status_code=404, detail="Ad content not found or is incomplete.")

    original_image_base64 = db_data.get("original_image_base64")
    product_name = db_data.get("product_name") or "Product"
    final_result_json = db_data.get("final_result_json")
    logo_image_base64 = db_data.get("logo_image_base64")

    # 필요한 데이터가 없으면 오류 처리
    if not original_image_base64 or not final_result_json:
        raise HTTPException(status_code=400, detail="Missing required data from database.")

    try:
        # DB에 저장된 final_result JSON (Qwen + Gemini 결과) 불러오기
        processed_data = json.loads(final_result_json)
        # Gemini가 생성한 배경 이미지를 Base64에서 바이트로 변환
        gemini_image_base64 = processed_data.get("final_image_base64")
        if not gemini_image_base64:
             raise HTTPException(status_code=400, detail="Gemini image is missing in DB result.")
        gemini_image_bytes = base64.b64decode(gemini_image_base64)
        
        qwen_layout = processed_data.get("qwen_analysis", {})

        # 로고 이미지 변환 (있을 경우)
        logo_image_bytes = base64.b64decode(logo_image_base64) if logo_image_base64 else None

    except (TypeError, ValueError, json.JSONDecodeError) as e:
        raise HTTPException(status_code=400, detail=f"Invalid data format from DB: {str(e)}")

    # 2. Text Renderer로 텍스트와 로고 합성
    try:
        copy_map = {"headline#0": product_name} # 예시로 제품명을 첫 텍스트로 설정
        
        final_ad_bytes = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: ad_text_renderer.render_ad(
                image_data=gemini_image_bytes,
                layout_json=qwen_layout,
                copy_map=copy_map,
                logo_data=logo_image_bytes,
                params={"target_ratio": 0.82, "line_spacing": 1.02, "stroke": 1}
            )
        )
        final_ad_base64 = base64.b64encode(final_ad_bytes).decode('utf-8')
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Text rendering failed: {str(e)}")

    # 3. 최종 결과 이미지 업데이트
    try:
        db_update_success = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: mysql_loader.update_final_ad_sync(request.ad_content_id, final_ad_base64)
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to update final image in DB: {str(e)}")

    if not db_update_success:
        return {"status": "Processing successful, but DB update failed.", "result": {"final_ad_base64": final_ad_base64}}

    return {"status": "All stages successful. Final ad saved to DB.", "result": {"final_ad_base64": final_ad_base64}}