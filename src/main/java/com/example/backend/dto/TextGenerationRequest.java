package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationRequest {

    // ✅ 필수
    private String product; // 제품명
    private String benefit; // 핵심 베네핏 1줄
    private String painPoint; // 타겟 상황/고통 1줄

    // ✅ 선택
    private String promotion; // 프로모션/가격
    private String toneGuide; // 금지 표현/톤 가이드

    // 💡 수동 Getter/Setter (원하면 다 지워도 됨 — Lombok이 이미 생성해줌)

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getBenefit() {
        return benefit;
    }

    public void setBenefit(String benefit) {
        this.benefit = benefit;
    }

    public String getPainPoint() {
        return painPoint;
    }

    public void setPainPoint(String painPoint) {
        this.painPoint = painPoint;
    }

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public String getToneGuide() {
        return toneGuide;
    }

    public void setToneGuide(String toneGuide) {
        this.toneGuide = toneGuide;
    }
}