// src/main/java/com/example/backend/entity/AdRun.java
package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ad_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===============================
    // 필수 관계
    // ===============================

    // 광고 콘텐츠
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private AdContent content;

    // 광고 집행 사용자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 광고 계정 (핵심 FK)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ad_account_id", nullable = false)
    private AdAccount adAccount;

    // ===============================
    // 스냅샷 / 외부 API 컨텍스트
    // ===============================

    // Meta Graph API용 광고 계정 ID (act_xxx)
    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    // Meta Page ID
    @Column(name = "page_id", nullable = false, length = 64)
    private String pageId;

    @Column(name = "link", nullable = false, length = 1024)
    private String link;

    @Column(name = "billing_event", length = 64)
    private String billingEvent;

    @Column(name = "optimization_goal", length = 64)
    private String optimizationGoal;

    @Column(name = "bid_strategy", length = 64)
    private String bidStrategy;

    @Column(name = "daily_budget", nullable = false, length = 32)
    private String dailyBudget;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    // ===============================
    // 광고 생성/수정 시점
    // ===============================

    @Column(name = "image_generated_at")
    private OffsetDateTime imageGeneratedAt;

    @Column(name = "ad_modified_at")
    private OffsetDateTime adModifiedAt;

    // ===============================
    // Meta 결과 ID
    // ===============================

    @Column(name = "campaign_id", length = 64)
    private String campaignId;

    @Column(name = "adset_id", length = 64)
    private String adsetId;

    @Column(name = "creative_id", length = 64)
    private String creativeId;

    @Column(name = "ad_id", length = 64)
    private String adId;

    // ===============================
    // 상태
    // ===============================

    @Column(name = "status", length = 32)
    private String status;

    // ===============================
    // 감사 필드
    // ===============================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
