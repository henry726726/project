package com.example.backend.dto;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponse {

    private String email;
    private String nickname; // 💡 추가: 사용자 닉네임 필드
    private Collection<? extends GrantedAuthority> authorities;
}