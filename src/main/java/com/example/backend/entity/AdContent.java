package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter; // 💡💡💡 @Setter 임포트 확인
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

// 광고 콘텐츠 엔티티 (JPA 테이블 매핑)
@Entity
@Table(name = "ad_contents")
@Getter
@Setter // 💡💡💡 @Setter 어노테이션이 있는지 확인합니다.
@NoArgsConstructor
@AllArgsConstructor
public class AdContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 문구 생성에 사용된 파라미터
    @Column(length = 255)
    private String product;
    @Column(length = 255)
    private String target;
    @Column(length = 255)
    private String purpose;
    @Column(length = 255)
    private String keyword;
    @Column(length = 255)
    private String duration;

    // 생성된 광고 문구
    @Column(columnDefinition = "TEXT")
    private String adText;

    @Lob
    @Column(name="original_image_base64", columnDefinition="LONGTEXT")
    private String originalImageBase64;

    // 합성된 이미지 (Base64 인코딩 문자열)
    @Lob
    @Column(name="generated_image_base64", columnDefinition="LONGTEXT")
    private String generatedImageBase64;

    // 누가 만들었는지 이메일 저장
    @Column(nullable = false, length = 255)
    private String userEmail;

    // 생성 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 💡💡💡 수동 Getter/Setter 메소드 추가 시작! 💡💡💡
    // Lombok @Getter/@Setter가 제대로 작동하지 않을 경우를 대비합니다.

    // Getters
    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public String getTarget() {
        return target;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getDuration() {
        return duration;
    }

    public String getAdText() {
        return adText;
    }

    public String getGeneratedImageBase64() {
        return generatedImageBase64;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setAdText(String adText) {
        this.adText = adText;
    }

    public void setGeneratedImageBase64(String generatedImageBase64) {
        this.generatedImageBase64 = generatedImageBase64;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 💡💡💡 수동 Getter/Setter 메소드 추가 끝! 💡💡💡

    // 저장 전 생성 시간을 자동으로 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}