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

✅ 로그인 시스템 실행 방법
1. SQL 창에서 먼저 코드를 입력하고 실행한다.
  CREATE DATABASE login_test;
  SHOW DATABASES;
  USE login_test;
  SHOW TABLES;
  SELECT * FROM users;
  ALTER TABLE users DROP COLUMN username;

2. 이 코드들을 터미널 창에서 입력하고 VS 코드에서 실행한다.
   ./gradlew clean build --refresh-dependencies
   ./gradlew bootRun

3. Postman을 실행한다. 아래는 Postman에서 해야할 것들이다.
  1) POST | http://localhost:8080/auth/signup
     Headers | Key:Content-type / Value: application/json
     Body > raw > JSON
    {
        "email": "test@example.com",
        "password": "password123!",
        "nickname": "테스트유저"
    }
     
  그리고 Send를 누른다. 주의) 만약 회원가입을 하려고 하는 이메일, 비밀번호, 닉네임이 중복될 경우 이미 사용 중이라고 뜬다.

  2) POST | http://localhost:8080/auth/login
     Headers | Key:Content-type / Value: application/json
     Body > raw > JSON
    {
      "email": "test@example.com",
      "password": "password123!"
    }
     
  그리고 Send를 누른다. 그럼 JWT 토큰이 나온다.

  3) GET | http://localhost:8080/user/me
     Authorization | Auth Type: Bearer Token
     Token 창에 아까 받은 JWT 토큰을 붙여넣기를 한다.
     그리고 Send를 누른다.
     
