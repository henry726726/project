package com.example.backend; // ServletInitializer의 패키지 경로

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import com.example.backend.metaads.MetaAdsApplication; // <--- 이제 이 import 문이 정확합니다!

public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // DemoApplication.class 대신 MetaAdsApplication.class로 변경
        return application.sources(MetaAdsApplication.class);
    }

}