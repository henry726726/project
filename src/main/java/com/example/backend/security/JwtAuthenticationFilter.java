package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 이 클래스는 HTTP 요청에서 JWT 토큰을 추출하고 유효성을 검증하여
// Spring Security 컨텍스트에 사용자 인증 정보를 설정하는 역할을 합니다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // JwtTokenProvider를 주입받기 위한 생성자
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 각 요청에 대해 필터 체인에서 실행될 메인 로직
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 💡💡💡 요청 URL 로그 추가 시작 💡💡💡
        System.out.println("JwtAuthenticationFilter: Request URL: " + request.getRequestURI());
        // 💡💡💡 요청 URL 로그 추가 끝 💡💡💡

        // 💡💡💡 resolveToken 결과 로그 추가 시작 💡💡💡
        // HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
        String token = jwtTokenProvider.resolveToken(request);
        // 추출된 토큰의 앞 20자리 또는 전체를 출력하여 확인합니다. (null일 경우 "null" 출력)
        System.out.println("JwtAuthenticationFilter: Resolved Token: " + (token != null ? token.substring(0, Math.min(token.length(), 20)) + "..." : "null"));
        // 💡💡💡 resolveToken 결과 로그 추가 끝 💡💡💡


        // 토큰이 null이 아니고 유효성 검증을 통과한 경우에만 인증 절차를 진행합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 토큰에서 사용자 이메일(subject)을 추출합니다.
            String email = jwtTokenProvider.getUserEmailFromToken(token);
            // 추출된 이메일로 UserDetailsService를 통해 UserDetails 객체를 로드합니다.
            UserDetails userDetails = jwtTokenProvider.getUserDetails(email);

            // UserDetails와 권한 정보를 바탕으로 인증 토큰을 생성합니다.
            // (Credentials는 토큰 기반 인증이므로 null로 설정합니다.)
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // 웹 인증 세부 정보를 설정합니다 (요청 IP, 세션 ID 등).
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            // 현재 보안 컨텍스트에 인증 객체를 설정합니다.
            // 이로써 해당 요청은 인증된 상태로 처리됩니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 💡💡💡 인증 성공 로그 추가 💡💡💡
            System.out.println("JwtAuthenticationFilter: Authentication successful for user: " + email);
            // 💡💡💡 인증 성공 로그 끝 💡💡💡

        } else {
            // 💡💡💡 토큰이 유효하지 않거나 없을 때 로그 추가 시작 💡💡💡
            // 토큰이 유효하지 않거나 추출되지 않은 경우, 인증 없이 다음 필터로 요청을 전달합니다.
            System.out.println("JwtAuthenticationFilter: Token is null or invalid. Proceeding without authentication.");
            // 💡💡💡 토큰이 유효하지 않거나 없을 때 로그 추가 끝 💡💡💡
        }

        // 필터 체인의 다음 필터로 요청을 전달합니다.
        filterChain.doFilter(request, response);
    }
}