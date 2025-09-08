# dags/ad_update_dag.py
from airflow import DAG
from airflow.operators.python import PythonOperator
from PIL import Image
from datetime import datetime, timedelta
import requests
import re
import os
import base64
import io

# ========================
# 환경 변수
# ========================
API_BASE = os.getenv("BACKEND_API_BASE", "http://localhost:8080")
# ⚠️ 테스트용 JWT 토큰 (고정)
JWT_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJxcXd3QG5hdmVyLmNvbSIsImF1dGgiOiJST0xFX1VTRVIiLCJpYXQiOjE3NTcyNzgwODAsImV4cCI6MTc1NzI4MTY4MH0.U2GDBQ3oFTBOtJ5_YT_FdSlOy4GMcnVgDE0ioqHbo6UYxBaD9NkSzckGLzLf8IS-8k2OL-hhIf4vWu1Isbr8kQ"
HEADERS = {"Authorization": f"Bearer {JWT_TOKEN}"}

# ========================
# 함수 정의
# ========================

def fetch_active_ad_runs(**context):
    """백엔드에서 집행 중인 광고 목록 조회"""
    url = f"{API_BASE}/meta/ad-runs/active?hoursSinceModified=24"
    resp = requests.get(url, headers=HEADERS)
    resp.raise_for_status()
    ad_runs = resp.json()
    print(f"✅ 활성 광고 개수: {len(ad_runs)}")
    print(f"DEBUG 응답 샘플: {ad_runs[:1]}")  # 첫 번째 광고 구조 확인
    context['ti'].xcom_push(key='ad_runs', value=ad_runs)


def decode_base64_image(base64_str):
    # 1) 줄바꿈/공백 제거
    cleaned = base64_str.strip().replace("\n", "").replace("\r", "")

    # 2) padding 보정
    if len(cleaned) % 4 != 0:
        cleaned += "=" * (4 - len(cleaned) % 4)

    return base64.b64decode(cleaned)


def clean_base64(b64_str: str) -> str:
    # Base64에 허용되지 않는 모든 문자 제거
    cleaned = re.sub(r'[^A-Za-z0-9+/=]', '', b64_str)
    # padding 보정
    if len(cleaned) % 4 != 0:
        cleaned += "=" * (4 - len(cleaned) % 4)
    return cleaned


def generate_ad_text(**context):
    """백엔드의 /api/generate 호출 → 새 광고 문구 생성"""
    ad_runs = context['ti'].xcom_pull(key='ad_runs', task_ids='fetch_ad_runs')
    updated_texts = {}

    for ad in ad_runs:
        payload = {
            "product": ad.get("product"),
            "target": ad.get("target"),
            "purpose": ad.get("purpose"),
            "keyword": ad.get("keyword"),
            "duration": ad.get("duration"),
        }

        url = f"{API_BASE}/api/generate"
        resp = requests.post(url, headers=HEADERS, json=payload)
        resp.raise_for_status()

        ad_texts = resp.json().get("adTexts", [])
        if ad_texts:
            new_text = ad_texts[0]
            ad_id = str(ad["adRunId"])  # str로 통일
            updated_texts[ad_id] = new_text
            print(f"✍️ 문구 생성 완료 (adRunId={ad_id}): {new_text}")
        else:
            print(f"⚠️ 문구 생성 실패: adRunId={ad['adRunId']}")

    print(f"✅ generate_ad_text 저장된 updated_texts: {updated_texts}")
    context['ti'].xcom_push(key='updated_texts', value=updated_texts)


def compose_image(**context):
    ad_runs = context['ti'].xcom_pull(key='ad_runs', task_ids='fetch_ad_runs')
    updated_texts = context['ti'].xcom_pull(key='updated_texts', task_ids='generate_texts')
    updated_images = {}

    for ad in ad_runs:
        ad_id = str(ad["adRunId"])
        new_text = updated_texts.get(ad_id)
        if not new_text:
            continue

        original_img_base64 = ad.get("originalImageBase64")
        if not original_img_base64:
            continue

        try:
            # Base64 정리 후 디코딩
            cleaned_b64 = clean_base64(original_img_base64)
            img_bytes = base64.b64decode(cleaned_b64)
        except Exception as e:
            print(f"❌ Base64 디코딩 실패 (adRunId={ad_id}): {e}")
            continue

        # Pillow로 이미지 확인
        try:
            image = Image.open(io.BytesIO(img_bytes))
            print(f"✅ Pillow 인식 성공 (format={image.format}, size={image.size})")
            mime_type = "image/jpeg"
            file_name = "input.jpg"
        except Exception as e:
            print(f"❌ Pillow 이미지 확인 실패 (adRunId={ad_id}): {e}")
            continue

        # API 호출
        url = f"{API_BASE}/api/compose"
        files = {"image": (file_name, img_bytes, mime_type)}
        data = {"text": new_text}
        resp = requests.post(url, headers={"Authorization": f"Bearer {JWT_TOKEN}"}, files=files, data=data)

        if resp.status_code == 200:
            updated_images[ad_id] = base64.b64encode(resp.content).decode("utf-8")
            print(f"🖼️ 이미지 합성 성공 (adRunId={ad_id}), 크기={len(resp.content)} bytes")
        else:
            print(f"❌ 이미지 합성 실패: {resp.status_code} - {resp.text}")

    context['ti'].xcom_push(key='updated_images', value=updated_images)


def update_ads(**context):
    """새 문구 + 이미지로 광고 업데이트"""
    ad_runs = context['ti'].xcom_pull(key='ad_runs', task_ids='fetch_ad_runs')
    updated_texts = context['ti'].xcom_pull(key='updated_texts', task_ids='generate_texts')
    updated_images = context['ti'].xcom_pull(key='updated_images', task_ids='compose_images')

    for ad in ad_runs:
        ad_run_id = str(ad["adRunId"])
        content_id = ad["contentId"]
        user_email = ad["userEmail"]

        new_text = updated_texts.get(ad_run_id)
        new_img_base64 = updated_images.get(ad_run_id)  # Base64 문자열
        if not new_text or not new_img_base64:
            print(f"⚠️ 업데이트 스킵 (adRunId={ad_run_id})")
            continue

        url = f"{API_BASE}/meta/update-ad"
        payload = {
            "adRunId": ad_run_id,
            "newContentId": content_id,
            "userEmail": user_email,
            "newText": new_text,
            "newImageBase64": new_img_base64  # ✅ Base64 문자열 그대로 전달
        }

        # JSON payload 전송
        resp = requests.post(url, headers={"Authorization": f"Bearer {JWT_TOKEN}"}, json=payload)

        if resp.status_code == 200:
            print(f"✅ 광고 업데이트 완료: adRunId={ad_run_id}")
        else:
            print(f"❌ 광고 업데이트 실패: {resp.status_code} - {resp.text}")

# ========================
# DAG 정의
# ========================
default_args = {
    "owner": "airflow",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="ad_update_dag",
    default_args=default_args,
    description="AdRun 기반 자동 광고 업데이트 DAG",
    schedule_interval="0 */6 * * *",
    start_date=datetime(2025, 8, 25),
    catchup=False,
    tags=["ads", "automation"],
) as dag:

    fetch_ad_runs = PythonOperator(
        task_id="fetch_ad_runs",
        python_callable=fetch_active_ad_runs,
    )

    generate_texts = PythonOperator(
        task_id="generate_texts",
        python_callable=generate_ad_text,
    )

    compose_images = PythonOperator(
        task_id="compose_images",
        python_callable=compose_image,
    )

    update_ads_task = PythonOperator(
        task_id="update_ads_task",
        python_callable=update_ads,
    )

    fetch_ad_runs >> generate_texts >> compose_images >> update_ads_task
