package com.example.backend.security;

import com.example.backend.service.BlacklistedTokenService; // 블랙리스트 서비스 임포트
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 임포트
import org.springframework.security.core.userdetails.UserDetailsService; // UserDetailsService 임포트
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Instant; // Instant 임포트 추가
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidityInMilliseconds;

    private Key key;
    private final UserDetailsService userDetailsService; // CustomUserDetailsService 주입
    private final BlacklistedTokenService blacklistedTokenService; // BlacklistedTokenService 주입

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * JWT 토큰을 생성합니다.
     * @param authentication 현재 인증된 사용자 정보
     * @return 생성된 JWT 토큰 문자열
     */
    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
            .setSubject(authentication.getName()) // 보통 사용자 ID 또는 이메일 (UserDetails의 username)
            .claim("auth", authorities) // 권한 정보
            .setIssuedAt(now) // 발행 시간
            .setExpiration(validity) // 만료 시간
            .signWith(key, SignatureAlgorithm.HS512) // 서명
            .compact();
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰 문자열을 추출합니다.
     * "Bearer " 접두사를 제거합니다.
     * @param request HttpServletRequest 객체
     * @return 추출된 JWT 토큰 문자열 (없으면 null)
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 부분 제거
        }
        return null;
    }

    /**
     * JWT 토큰에서 사용자 이메일(subject)을 추출합니다.
     * @param token JWT 토큰 문자열
     * @return 추출된 사용자 이메일
     */
    public String getUserEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }

    /**
     * JWT 토큰에서 만료 시간(Instant)을 추출합니다.
     * @param token JWT 토큰 문자열
     * @return 토큰의 만료 시간 (Instant 객체), 파싱 실패 시 null
     */
    public Instant getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            return claims.getExpiration().toInstant(); // Date를 Instant로 변환
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰 파싱 실패 또는 유효하지 않은 경우
            return null;
        }
    }

    /**
     * 이메일을 통해 UserDetails 객체를 로드합니다.
     * JwtAuthenticationFilter에서 JWT 토큰의 유효성 검사 후 사용자 정보를 SecurityContext에 설정하기 위해 사용됩니다.
     * @param email 사용자 이메일 (JWT 토큰의 subject)
     * @return UserDetailsService를 통해 로드된 UserDetails 객체
     */
    public UserDetails getUserDetails(String email) {
        return userDetailsService.loadUserByUsername(email);
    }

    /**
     * JWT 토큰의 유효성을 검사합니다.
     * @param token 유효성을 검사할 JWT 토큰 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            // 1. 블랙리스트에 있는지 먼저 확인합니다.
            if (blacklistedTokenService.isTokenBlacklisted(token)) {
                System.out.println("블랙리스트에 있는 토큰입니다.");
                return false;
            }

            // 2. 토큰 파싱 및 유효성 검사
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            System.out.println("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            System.out.println("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            System.out.println("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            System.out.println("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}