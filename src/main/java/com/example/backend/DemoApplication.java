package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {  // 혹은 BackendApplication 이름 상관없음
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}