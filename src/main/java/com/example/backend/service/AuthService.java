/*
package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenService blacklistedTokenService;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일이나 비밀번호입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getEmail(), encodedPassword, request.getNickname());
        userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            return jwtTokenProvider.createToken(authentication);
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // 기존 logout 메서드
    public boolean logout(String token) {
        Instant expiration = jwtTokenProvider.getExpirationDateFromToken(token);
        if (expiration == null) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        blacklistedTokenService.blacklistToken(token, expiration);
        return true;
    }
}
*/