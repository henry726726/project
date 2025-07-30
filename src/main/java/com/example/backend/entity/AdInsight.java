package com.example.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter; // Lombok 추가
import lombok.Setter; // Lombok 추가
import lombok.NoArgsConstructor; // Lombok 추가

@Entity
@Getter // Lombok Getters
@Setter // Lombok Setters
@NoArgsConstructor // Lombok 기본 생성자
public class AdInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 💡 기존 private String adId; 필드 대신 Ad 엔티티와의 ManyToOne 관계 추가
    @ManyToOne(fetch = FetchType.LAZY) // 여러 AdInsight가 하나의 Ad에 속함
    @JoinColumn(name = "ad_id") // AdInsight 테이블에 ad_id 외래키 컬럼 생성
    private Ad ad; // Ad 엔티티 자체를 참조

    private String age;
    private String gender;

    private int impressions;
    private int clicks;
    private BigDecimal spend;
    private int reach;
    private BigDecimal cpc;
    private BigDecimal ctr;
    private BigDecimal frequency;

    private LocalDate date;

    // Lombok으로 게터/세터 및 생성자를 대체했습니다.
    // 기존 수동 작성된 게터/세터는 @Getter/@Setter 어노테이션으로 대체됩니다.
}