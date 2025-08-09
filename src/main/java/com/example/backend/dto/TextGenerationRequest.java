package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter // <-- 이 어노테이션이 있다면 Lombok이 Getter를 자동 생성
@Setter // <-- 이 어노테이션이 있다면 Lombok이 Setter를 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 자동 생성
public class TextGenerationRequest {
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;

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
}