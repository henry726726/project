// src/main/java/com/example/backend/security/JwtAuthenticationFilter.java (가정)

package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component; // 💡💡💡 이거 임포트하세요!

import org.springframework.web.filter.OncePerRequestFilter; // OncePerRequestFilter 임포트

import java.io.IOException;

@Component // 💡💡💡 여기! 이 어노테이션을 추가해 줘야 합니다! 💡💡💡
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider; // JwtTokenProvider 주입

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtTokenProvider.resolveToken(request); // 요청에서 토큰 추출

        if (token != null && jwtTokenProvider.validateToken(token)) { // 토큰 유효성 검사
            String email = jwtTokenProvider.getUserEmailFromToken(token); // 토큰에서 이메일 추출
            UserDetails userDetails = jwtTokenProvider.getUserDetails(email); // 이메일로 UserDetails 로드

            // 인증 정보 설정
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response); // 다음 필터로 요청 전달
    }
}