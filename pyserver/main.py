from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from contextlib import asynccontextmanager
import asyncio
from concurrent.futures import ThreadPoolExecutor
import os
import base64
import json
import io
import pymysql
from dotenv import load_dotenv
from typing import Optional

# dotenv를 사용하여 .env 파일의 환경 변수를 로드합니다.
# 이 줄은 반드시 다른 모듈 임포트 및 초기화 코드보다 먼저 와야 합니다.
load_dotenv()

# 프로젝트의 다른 모듈들을 임포트합니다.
# 이 모듈들은 환경 변수에 의존하므로 load_dotenv() 이후에 임포트해야 합니다.
from mysql_module import MySQLLoader
from qwen_module import QwenProcessor
from nanobanana_module import GeminiImageGenerator
from ad_text_render_module import AdTextRenderer

# 모듈 초기화는 애플리케이션 시작 시 한 번만 수행되도록 lifespan 내부에 배치합니다.
# 이렇게 하면 시작 실패 시 애플리케이션이 정상적으로 시작되지 않습니다.
qwen_processor: Optional[QwenProcessor] = None
gemini_generator: Optional[GeminiImageGenerator] = None
ad_text_renderer: Optional[AdTextRenderer] = None
mysql_loader: Optional[MySQLLoader] = None

# API 요청을 위한 Pydantic 모델
class AdContentRequest(BaseModel):
    ad_content_id: int

# 스레드풀 설정
executor = ThreadPoolExecutor(max_workers=5)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 애플리케이션의 시작/종료 이벤트를 관리합니다.
    모든 전역 모듈 및 리소스 초기화가 이곳에서 이루어집니다.
    """
    global qwen_processor, gemini_generator, ad_text_renderer, mysql_loader

    required_vars = ["MYSQL_HOST", "MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DB",
                     "GOOGLE_CLOUD_PROJECT", "GOOGLE_CLOUD_LOCATION", "GOOGLE_GENAI_USE_VERTEXAI"]
    if not all(os.getenv(var) for var in required_vars):
        print("❌ 필수 환경 변수 중 일부가 누락되었습니다.")
        raise RuntimeError("Missing required environment variables.")

    try:
        print("🚀 모든 모듈을 초기화합니다...")
        qwen_processor = QwenProcessor()
        gemini_generator = GeminiImageGenerator()
        ad_text_renderer = AdTextRenderer()
        mysql_loader = MySQLLoader()
        print("✅ 모든 모듈 초기화 성공.")
    except Exception as e:
        print(f"⛔ 모듈 초기화 실패: {e}")
        # 모듈 초기화 실패 시 애플리케이션 시작을 중단합니다.
        raise RuntimeError("Module initialization failed.")

    yield  # 애플리케이션 실행

    # 애플리케이션 종료 시 실행되는 정리(shutdown) 로직
    print("🧹 애플리케이션을 종료합니다...")
    if mysql_loader:
        mysql_loader.close()
    print("✅ 애플리케이션 종료 완료.")

app = FastAPI(lifespan=lifespan)

@app.post("/generate-ad-with-text")
async def generate_ad_with_text(request: AdContentRequest):
    """
    ID를 받아 DB에서 모든 정보를 로드하고, Qwen, Gemini, Text Renderer를
    순차적으로 실행하여 최종 광고 이미지를 생성하고 DB에 저장하는 API.
    """
    if not all([qwen_processor, gemini_generator, ad_text_renderer, mysql_loader]):
        raise HTTPException(status_code=503, detail="Service is not ready. Modules failed to initialize.")

    try:
        print(f"🔍 ad_content_id {request.ad_content_id} 데이터베이스에서 로드 중...")
        db_data = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: mysql_loader.get_ad_content_sync(request.ad_content_id)
        )
        print(f"✅ 데이터 로드 성공. 데이터: {db_data}")
    except Exception as e:
        print(f"⛔ 데이터베이스 쿼리 실패: {e}")
        raise HTTPException(status_code=500, detail=f"Database query failed: {str(e)}")

    if not db_data:
        print(f"⛔ ad_content_id {request.ad_content_id}에 해당하는 데이터가 데이터베이스에 없습니다.")
        raise HTTPException(status_code=404, detail="Ad content not found or is incomplete.")
    else:
        print(f"✅ 데이터 찾음. Qwen 분석 결과 로드 중...")

    product_name = db_data.get("product")
    original_image_base64 = db_data.get("original_image_base64")
   
    try:
        print("🤖 Qwen 모듈을 통해 이미지 레이아웃 분석 및 광고 문구 생성 중...")
        qwen_layout = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: qwen_processor.generate_ad_copy(original_image_base64, product_name)
        )
        print(f"✅ Qwen 분석 완료. 결과: {qwen_layout}")
    except Exception as e:
        print(f"⛔ Qwen 모듈 실행 실패: {e}")
        raise HTTPException(status_code=500, detail=f"Qwen module failed: {str(e)}")
    
    # 2. Gemini 이미지 생성
    try:
        print("🖼️ Gemini 모듈로 광고 이미지 생성 중...")
        gemini_image_base64 = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: gemini_generator.generate_ad_image_sync(qwen_layout, product_name)
        )
        print(f"✅ Gemini 이미지 생성 완료. Base64 길이: {len(gemini_image_base64) if gemini_image_base64 else 0}")
        if not gemini_image_base64:
            print("⛔ Gemini 이미지 생성 결과가 비어있습니다.")
            raise HTTPException(status_code=500, detail="Gemini image generation returned an empty result.")

        gemini_image_bytes = base64.b64decode(gemini_image_base64)
        logo_image_base64 = db_data.get("logo_image_base64")
        logo_image_bytes = base64.b64decode(logo_image_base64) if logo_image_base64 else None

    except (TypeError, ValueError, json.JSONDecodeError) as e:
        print(f"⛔ DB에서 가져온 데이터 형식 오류: {e}")
        raise HTTPException(status_code=400, detail=f"Invalid data format from DB: {str(e)}")

    # 3. Text Renderer로 텍스트와 로고 합성
    try:
        print("🎨 Text Renderer로 텍스트 합성 중...")
        copy_map = {"headline#0": product_name}
        
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
        print("✅ 텍스트 합성 완료.")
    except Exception as e:
        print(f"⛔ 텍스트 렌더링 실패: {e}")
        raise HTTPException(status_code=500, detail=f"Text rendering failed: {str(e)}")

    # 4. 최종 결과 이미지 업데이트
    try:
        print("💾 최종 이미지 데이터베이스에 저장 중...")
        db_update_success = await asyncio.get_event_loop().run_in_executor(
            executor,
            lambda: mysql_loader.update_generated_image_sync(request.ad_content_id, final_ad_base64)
        )
        print(f"✅ 데이터베이스 업데이트 성공: {db_update_success}")
    except Exception as e:
        print(f"⛔ 최종 이미지 데이터베이스 업데이트 실패: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to update final image in DB: {str(e)}")

    if not db_update_success:
        return {"status": "Processing successful, but DB update failed.", "result": {"final_ad_base64": final_ad_base64}}

    return {"status": "All stages successful. Final ad saved to DB.", "result": {"final_ad_base64": final_ad_base64}}