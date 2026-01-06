package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_contents", indexes = {
        // 🔸 FK 기준 인덱스 (추후 메인)
        @Index(name = "idx_ad_contents_user_id_created", columnList = "user_id, createdAt"),
        // 🔸 기존 userEmail 인덱스 유지 (하위 호환)
        @Index(name = "idx_ad_contents_user_email_created", columnList = "userEmail, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class AdContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /*
     * =========================
     * ✅ 사용자 기준 (핵심)
     * =========================
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*
     * =========================
     * 🔸 보조 식별자 (로그/표시용)
     * =========================
     */
    @Column(nullable = false, length = 255)
    private String userEmail;

    /*
     * =========================
     * 광고 메타 정보
     * =========================
     */
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

    /*
     * =========================
     * 광고 콘텐츠
     * =========================
     */
    @Lob
    private String adText;

    @Lob
    private String originalImageBase64;

    @Lob
    private String generatedImageBase64;

    /*
     * =========================
     * 생성 시각
     * =========================
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
