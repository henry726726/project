package com.example.backend; // <--- 이 패키지 경로를 다시 한번 정확히 확인하세요.

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.security.oauth2.client.registration.facebook.client-id=test-dummy-client-id",
    "spring.security.oauth2.client.registration.facebook.client-secret=test-dummy-client-secret"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // 이 테스트는 Spring 컨텍스트가 성공적으로 로드되는지 확인합니다.
    }
}