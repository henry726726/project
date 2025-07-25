package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 광고 콘텐츠 저장을 위한 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveAdContentRequest {
    // TextGenerator에서 사용된 파라미터 (광고 문구의 생성 조건)
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;

    // 생성된 광고 문구
    private String adText;

    // 합성된 이미지 (Base64 인코딩된 문자열)
    private String generatedImageBase64;
}