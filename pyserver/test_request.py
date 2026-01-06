import requests

url = "http://127.0.0.1:8000/extract-keyword"

# 여러 테스트 문장
tests = [
    "실버 꽃모양 목걸이",
    "검은색 지갑",
    "빨간 운동화",
    "골드 다이아몬드 반지",
    "가죽 가방",
    "전자 시계"
]

for text in tests:
    payload = {"text": text}
    response = requests.post(url, json=payload)
    result = response.json()
    print(f"입력: {text} → 추출: {result['keyword']}")