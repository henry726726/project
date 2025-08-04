// src/main/java/com/example/backend/config/SecurityConfig.java (전체 코드)
package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.JwtTokenProvider;
// import com.example.backend.security.CustomOAuth2SuccessHandler; // 💡 CustomOAuth2SuccessHandler는 삭제되었으므로 이 import는 지웁니다.
// import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy; // authorizedClientService 빈이 삭제되면 더 이상 필요없을 수 있음
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// 💡 추가된 import 구문들 (OAuth2 관련 임포트는 authorizedClientService 빈을 삭제한다면 함께 삭제)
// import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
// import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
// import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

// AccessTokenRepository가 필요하면 이 import는 유지. (단, 사용되지 않으면 경고 뜰 것)
// import com.example.backend.repository.AccessTokenRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
// @RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;
        private final UserDetailsService userDetailsService;
        // private final CustomOAuth2SuccessHandler successHandler; // 💡 삭제되었으므로 이 필드도 지웁니다.

        // 💡💡💡 successHandler 필드를 제거했으므로 생성자도 변경됩니다. 💡💡💡
        public SecurityConfig(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
            this.jwtTokenProvider = jwtTokenProvider;
            this.userDetailsService = userDetailsService;
        }

        @Bean
        // 💡💡💡 HttpSecurity 뒤의 파라미터들 (authorizedClientService, accessTokenRepository) 제거 💡💡💡
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // CustomOAuth2SuccessHandler와 AccessTokenRepository 사용 로직은 삭제되었으므로, 여기에서 CustomOAuth2SuccessHandler를 생성하는 부분도 지웁니다.
                // 이 부분을 삭제하고, .oauth2Login(...) 설정 라인을 주석 처리합니다.

                http
                                .httpBasic(httpBasicConfig -> httpBasicConfig.disable())
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/auth/**",
                                                                "/api/register",
                                                                "/api/generate",
                                                                "/meta/**",
                                                                "/", "/login**", "/error**") // "/login**"은 이제 일반 로그인 페이지를 의미
                                                .permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .anyRequest().authenticated())
                                // 💡💡💡 이 라인 (oauth2Login 설정)을 완전히 주석 처리하거나 삭제합니다. 💡💡💡
                                // .oauth2Login(oauth -> oauth.successHandler(customSuccessHandler))
                                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setPasswordEncoder(passwordEncoder());
                provider.setUserDetailsService(userDetailsService);
                return provider;
        }

        // 💡💡💡 OAuth2AuthorizedClientService 빈 정의도 완전히 주석 처리하거나 삭제합니다. 💡💡💡
        // 이 빈은 CustomOAuth2SuccessHandler를 위해 필요했기 때문입니다.
        // @Bean
        // @Lazy
        // public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        //     return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        // }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:3000"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}