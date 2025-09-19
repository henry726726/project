# 이 스크립트를 실행하기 전에 `pip install requests`를 먼저 실행하세요.

import requests
import json

# FastAPI 서버의 URL
API_URL = "http://127.0.0.1:8000/generate-ad-with-text"

# 예제 요청 데이터
# 이 ad_content_id는 MySQL 데이터베이스에 실제로 존재하는 값이어야 합니다.
# 존재하지 않는 ID를 사용하면 404 에러가 반환됩니다.
request_data = {
    "ad_content_id": 26
}

# 요청 헤더 (선택 사항, API가 필요로 할 경우)
headers = {
    "Content-Type": "application/json"
}

print(f"Sending POST request to {API_URL}")
print(f"Request data: {json.dumps(request_data, indent=2)}")

try:
    # API에 POST 요청 보내기
    response = requests.post(API_URL, data=json.dumps(request_data), headers=headers)
    
    # 응답 상태 코드 확인
    response.raise_for_status() # HTTP 에러가 발생하면 예외를 던집니다.
    
    # JSON 응답 출력
    response_json = response.json()
    print("\nAPI Response:")
    print(json.dumps(response_json, indent=2, ensure_ascii=False))

except requests.exceptions.RequestException as e:
    # 요청 실패 시 오류 메시지 출력
    print(f"\nError: {e}")
    if e.response:
        print(f"Status Code: {e.response.status_code}")
        print(f"Response Body: {e.response.text}")
except json.JSONDecodeError:
    # JSON 파싱 실패 시 오류 메시지 출력
    print("\nError: Could not decode JSON response.")
    print(f"Response Body: {response.text}")
