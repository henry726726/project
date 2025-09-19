package com.example.backend.config; // ⬅️ 이 부분은 본인의 Spring Boot 프로젝트 패키지 구조에 맞게 수정해주세요!

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 이 클래스가 스프링의 설정(Configuration) 파일임을 나타냅니다.
public class WebConfig {

    @Bean // 이 메서드가 반환하는 객체(WebMvcConfigurer)를 스프링 컨테이너에 빈으로 등록합니다.
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API 경로 (예: /api/report/pdf, /auth/login 등)에 대해 CORS를 적용합니다.
                        .allowedOrigins("http://localhost:3000") // ⬅️ 중요! 여기에 React 앱이 실행되는 주소와 포트를 정확히 입력하세요.
                                                                  // 여러 개를 허용하려면 쉼표(,)로 구분할 수 있습니다.
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드를 지정합니다. (OPTIONS는 Preflight 요청에 필요)
                        .allowedHeaders("*") // 모든 종류의 HTTP 헤더를 허용합니다. (예: Content-Type, Authorization 등)
                        .allowCredentials(true) // ⬅️ 중요! 자격 증명(쿠키, HTTP 인증 등)을 함께 보낼 수 있도록 허용합니다.
                                                // React fetch 요청에서도 `credentials: 'include'` 옵션을 사용해야 합니다.
                        .maxAge(3600); // Preflight 요청(사전 점검 요청)의 결과를 1시간(3600초) 동안 캐시합니다.
            }
        };
    }
}