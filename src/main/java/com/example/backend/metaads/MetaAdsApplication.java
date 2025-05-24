package com.example.backend.metaads; // <--- 이 패키지 선언이 핵심입니다.

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MetaAdsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetaAdsApplication.class, args);
    }
}