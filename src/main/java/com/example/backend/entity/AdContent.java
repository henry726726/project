package com.example.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 🔴 여기! @Table(name = "ad_content")를 @Table(name = "ad_contents")로 변경합니다!
@Table(name = "ad_contents") // 🔴 실제 데이터베이스 테이블 이름에 맞게 's' 추가!
public class AdContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String adId;

    @Column(columnDefinition = "TEXT")
    private String generatedImageBase64;
    
    @Column(columnDefinition = "TEXT")
    private String originalImageBase64;

    @Column
    private String product;

    @Column
    private String target;

    @Column
    private String purpose;

    @Column
    private String keyword;

    @Column
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String adText;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column
    private String userEmail;

    @Column
    private LocalDateTime createdAt;
}