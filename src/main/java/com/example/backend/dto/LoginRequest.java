package com.example.backend.dto;

import lombok.Getter;     // Lombok @Getter 임포트
import lombok.Setter;     // Lombok @Setter 임포트
import lombok.NoArgsConstructor; // Lombok @NoArgsConstructor 임포트
import lombok.AllArgsConstructor; // Lombok @AllArgsConstructor 임포트

@Getter
@Setter
@NoArgsConstructor // 기본 생성자 자동 생성 (필요하다면)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 자동 생성
public class LoginRequest {
    private String email;
    private String password;

    // 💡💡💡 Lombok이 생성해주던 Getter/Setter 메소드들을 직접 추가합니다. 💡💡💡
    // 모든 필드에 대해 Getter/Setter를 추가해야 합니다.
    // 현재 오류는 getEmail()과 관련된 것이지만, 전체를 추가하는 게 좋습니다.

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}