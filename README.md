# project
본 프로젝트를 실행하기위해서는 application.properties에 gpt api를 작성하고 백엔드를 먼저 실행하고, 이후 프론트를 실행시켜 결과물을 확인해야함.

gpt api는 노션의 ai엔지니어 설명란에 있음


# 백엔드 시작하는 방법

cd demo
./gradlew bootRun

# 프론트 시작하는방법

## 최초 실행시
cd src/main/frontend
npm install
npm start

기본적으로 브라우저에서 http://localhost:3000에 열림


✅ API 연동 구조
React → Spring Boot → OpenAI

프론트: POST http://localhost:8080/api/generate

백엔드: GPT API 호출 후 응답 전달

응답 예시: "output": "[\"문구1\", \"문구2\"]"


✅ 주의사항
항목	            내용
CORS 허용	        백엔드 컨트롤러에 @CrossOrigin(origins = "*") 추가
백엔드 먼저 실행	React가 백엔드에 요청하기 때문에 순서 중요
API 키	            .env 또는 application.properties에서 관리 (OPENAI_API_KEY)
포트 충돌 주의	    백엔드 8080, 프론트 3000에서 실행되도록 유지

