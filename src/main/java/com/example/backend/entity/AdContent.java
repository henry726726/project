// 기존 AdContent.java 코드는 변화 없음 (이미 Lombok 적용되어 있음)
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime; // LocalDateTime 임포트 명시적으로 추가

@Entity
@Table(name = "ad_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 문구 생성에 사용된 파라미터
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;

    // 생성된 광고 문구
    @Column(columnDefinition = "TEXT")
    private String adText;

    // 합성된 이미지 (Base64 인코딩 문자열)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedImageBase64;

    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}