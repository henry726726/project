// src/main/java/com/example/backend/dto/AdGenerationParameters.java
package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 광고 문구 생성 요청 파라미터 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdGenerationParameters {
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;
}