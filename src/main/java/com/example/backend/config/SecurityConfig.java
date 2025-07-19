package com.example.backend.config;

import com.example.backend.security.CustomOAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomOAuth2SuccessHandler successHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf().disable() // CSRF 비활성화
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", "/login**", "/error**",

                                                                // ✅ 여기에 CORS 허용하고 싶은 엔드포인트 모두 추가
                                                                "/api/generate", // GPT 광고 문구 생성
                                                                "/meta/create",
                                                                "/meta/sync-ads",
                                                                "/meta/insight",
                                                                "/meta/insight/test",
                                                                "/meta/test-update")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .cors() // ✅ CORS 활성화
                                .and()
                                .oauth2Login(oauth -> oauth
                                                .successHandler(successHandler));

                return http.build();
        }
}
