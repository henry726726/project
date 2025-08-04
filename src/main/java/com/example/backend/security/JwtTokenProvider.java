package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest; // 💡HttpServletRequest는 JwtAuthenticationFilter에서 필요하고, JwtTokenProvider에서는 resolveToken에서만 사용 (다른 용도로는 제거 가능)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidityInMilliseconds;

    private Key key;
    private final UserDetailsService userDetailsService;

    public JwtTokenProvider(UserDetailsService userDetailsService,
                            @Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.expiration}") long tokenValidityInMilliseconds) {
        this.userDetailsService = userDetailsService;
        this.secretKey = secretKey;
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        System.out.println("JWTTokenProvider Constructor called. secretKey (received): " + secretKey);
    }


    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        System.out.println("JWT Secret Key (init): " + secretKey);
        System.out.println("JWT Key (init byte length): " + secretKey.getBytes().length * 8 + " bits");
    }

    // JWT 토큰 생성
    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        String token = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        System.out.println("Generated JWT: " + token);
        return token;
    }

    // HTTP 요청 헤더에서 JWT 토큰 추출 (JwtAuthenticationFilter에서 호출)
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        System.out.println("JwtTokenProvider: raw Authorization Header: " + bearerToken); // 💡💡💡 raw 헤더 값 확인

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            System.out.println("JwtTokenProvider: Token found, starts with Bearer."); // 💡💡💡 Bearer 확인됨
            return bearerToken.substring(7);
        }
        System.out.println("JwtTokenProvider: Token not found or does not start with Bearer."); // 💡💡💡 조건 불충족
        return null;
    }

    // JWT 토큰에서 사용자 이메일(subject) 추출 (JwtAuthenticationFilter에서 호출)
    public String getUserEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 이메일을 통해 UserDetails 객체 로드 (JwtAuthenticationFilter에서 호출)
    public UserDetails getUserDetails(String email) {
        return userDetailsService.loadUserByUsername(email);
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            System.out.println("Validating token: " + token);
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            System.out.println("Token is valid.");
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            System.out.println("잘못된 JWT 서명입니다: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("만료된 JWT 토큰입니다: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("지원되지 않는 JWT 토큰입니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("JWT 토큰이 잘못되었습니다 (클레임 문자열이 비었거나 형식 오류): " + e.getMessage());
        }
        System.out.println("Token is invalid.");
        return false;
    }
}