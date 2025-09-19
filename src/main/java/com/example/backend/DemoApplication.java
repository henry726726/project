package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 🔴 추가

@SpringBootApplication
@EnableScheduling // 🔴 이 어노테이션을 추가하여 스케줄링 기능을 활성화합니다.
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}