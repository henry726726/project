package com.example.metaads.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API 테스트를 위해 CSRF 비활성화 (프로덕션 환경에서는 CSRF 보호 활성화 고려)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/oauth2/**", "/login**", "/error").permitAll() // 초기 페이지 및 OAuth2 리디렉션 허용
                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/api/test", true) // 로그인 성공 시 테스트 API 호출 페이지로 이동
                .failureUrl("/error") // 로그인 실패 시 에러 페이지로 이동
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/") // 로그아웃 성공 시 홈으로 이동
            );
        return http.build();
    }
}