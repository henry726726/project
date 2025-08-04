package com.example.backend.dto;

import lombok.Getter;     // Lombok @Getter 임포트
import lombok.Setter;     // Lombok @Setter 임포트
import lombok.NoArgsConstructor; // Lombok @NoArgsConstructor 임포트
import lombok.AllArgsConstructor; // Lombok @AllArgsConstructor 임포트

@Getter
@Setter
@NoArgsConstructor // 기본 생성자 자동 생성 (필요하다면)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 자동 생성
public class ChangePasswordRequest {
    private String currentPassword; // 현재 비밀번호
    private String newPassword;     // 새 비밀번호
}