package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 광고 콘텐츠 저장 요청 DTO
@Getter // Lombok: 모든 필드에 대한 Getter 자동 생성
@Setter // Lombok: 모든 필드에 대한 Setter 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor // Lombok: 모든 필드를 인자로 받는 생성자 자동 생성
public class SaveAdContentRequest {
    // TextGenerator.jsx와 ImageGenerator.jsx에서 savePayload로 보낸 데이터 구조와 일치해야 합니다.
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;
    private String adText; // 광고 문구
    private String generatedImageBase64; // Base64 인코딩된 이미지
    private String originalImageBase64;

    // 💡💡💡 수동으로 Getter/Setter 메소드를 추가합니다. 💡💡💡
    // Lombok이 작동하지 않는 환경을 대비한 확실한 해결책입니다.

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getAdText() {
        return adText;
    }

    public void setAdText(String adText) {
        this.adText = adText;
    }

    public String getGeneratedImageBase64() {
        return generatedImageBase64;
    }

    public void setGeneratedImageBase64(String generatedImageBase64) {
        this.generatedImageBase64 = generatedImageBase64;
    }
}