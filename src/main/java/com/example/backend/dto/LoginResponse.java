// src/main/java/com/example/backend/dto/LoginResponse.java (수정된 전체 코드)
package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // <-- 이 생성자는 token과 message 둘 다 필요
public class LoginResponse {
    private String token;
    private String message;

    // 💡💡💡 추가: String token만 받는 생성자 💡💡💡
    public LoginResponse(String token) {
        this.token = token;
        this.message = "로그인 성공"; // 기본 메시지 설정 (선택 사항)
    }
}