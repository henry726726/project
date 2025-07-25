package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 광고 콘텐츠 엔티티 (JPA 테이블 매핑)
@Entity
@Table(name = "ad_contents") // 데이터베이스 테이블 이름
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 ID
    private Long id;

    // 문구 생성에 사용된 파라미터
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;

    // 생성된 광고 문구
    @Column(columnDefinition = "TEXT") // TEXT 타입으로 길게 저장 가능
    private String adText;

    // 합성된 이미지 (Base64 인코딩 문자열)
    @Lob // Large Object (긴 문자열이나 바이너리 데이터를 저장할 때 사용)
    @Column(columnDefinition = "LONGTEXT") // MySQL의 LONGTEXT 타입으로 매핑 (매우 긴 Base64 문자열 저장)
    private String generatedImageBase64;

    @Column(updatable = false) // 생성 시간은 업데이트되지 않음
    private java.time.LocalDateTime createdAt; // 생성 시간

    // 저장 전 생성 시간을 자동으로 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}