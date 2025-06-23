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
                                .csrf().disable() // CSRF 비활성화 (테스트용)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/login**",
                                                                "/error**",
                                                                "/meta/create",
                                                                "/meta/sync-ads",
                                                                "/meta/insight",
                                                                "/meta/insight/test",
                                                                "/meta/test-update" // ✅ 여기에 추가
                                                ).permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .successHandler(successHandler));

                return http.build();
        }

}
