// src/main/java/com/example/backend/dto/TextGenerationResponse.java
package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

// 광고 문구 생성 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationResponse {
    private List<String> adTexts; // 생성된 광고 문구 목록
}