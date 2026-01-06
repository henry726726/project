package com.example.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ad_insights", indexes = {
        // 🔸 AdRun 기준 조회 (핵심)
        @Index(name = "idx_ad_insight_ad_run_date", columnList = "ad_run_id, date"),
        // 🔸 외부 API(adId) 기준 조회 (보조)
        @Index(name = "idx_ad_insight_ad_id_date", columnList = "ad_id, date")
})
public class AdInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * =========================
     * ✅ 내부 기준 (핵심)
     * =========================
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ad_run_id", nullable = false)
    private AdRun adRun;

    /*
     * =========================
     * 🔸 외부 API 식별자 (보조)
     * =========================
     */
    @Column(name = "ad_id", length = 64)
    private String adId;

    /*
     * =========================
     * breakdown 정보
     * =========================
     */
    @Column(length = 32)
    private String age;

    @Column(length = 16)
    private String gender;

    /*
     * =========================
     * 성과 지표
     * =========================
     */
    private int impressions;
    private int clicks;

    @Column(precision = 19, scale = 4)
    private BigDecimal spend;

    private int reach;

    @Column(precision = 19, scale = 4)
    private BigDecimal cpc;

    @Column(precision = 19, scale = 4)
    private BigDecimal ctr;

    @Column(precision = 19, scale = 4)
    private BigDecimal frequency;

    private LocalDate date;

    /*
     * =========================
     * Getter / Setter
     * =========================
     */

    public Long getId() {
        return id;
    }

    public AdRun getAdRun() {
        return adRun;
    }

    public void setAdRun(AdRun adRun) {
        this.adRun = adRun;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getImpressions() {
        return impressions;
    }

    public void setImpressions(int impressions) {
        this.impressions = impressions;
    }

    public int getClicks() {
        return clicks;
    }

    public void setClicks(int clicks) {
        this.clicks = clicks;
    }

    public BigDecimal getSpend() {
        return spend;
    }

    public void setSpend(BigDecimal spend) {
        this.spend = spend;
    }

    public int getReach() {
        return reach;
    }

    public void setReach(int reach) {
        this.reach = reach;
    }

    public BigDecimal getCpc() {
        return cpc;
    }

    public void setCpc(BigDecimal cpc) {
        this.cpc = cpc;
    }

    public BigDecimal getCtr() {
        return ctr;
    }

    public void setCtr(BigDecimal ctr) {
        this.ctr = ctr;
    }

    public BigDecimal getFrequency() {
        return frequency;
    }

    public void setFrequency(BigDecimal frequency) {
        this.frequency = frequency;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
