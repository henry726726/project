package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean; // 💡 임포트 추가
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService; // 💡 임포트 추가

// 아래의 @EnableAutoConfiguration과 @ComponentScan 필터링은 이제 제거합니다.
// 이 부분은 오히려 혼란을 주거나 충분히 빈을 제외하지 못했을 수 있습니다.
// @EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
// @ComponentScan(excludeFilters = @ComponentScan.Filter(
//     type = FilterType.ASSIGNABLE_TYPE,
//     classes = CustomOAuth2SuccessHandler.class
// ))

@SpringBootTest
@ActiveProfiles("test") // (ddl-auto=none을 위해 추가했다면 유지)
class DemoApplicationTests {

    // 💡💡💡 OAuth2AuthorizedClientService를 목(Mock) 빈으로 등록합니다.
    // 이렇게 하면 Spring 컨테이너가 이 타입의 빈을 찾을 때 실제 구현 대신 목 객체를 제공하여
    // 의존성 문제를 해결합니다.
    @MockBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Test
    void contextLoads() {
        // 스프링 컨텍스트가 성공적으로 로드되면 테스트 통과
    }
}