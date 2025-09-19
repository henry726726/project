package com.example.backend.repository;

import com.example.backend.entity.AdContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdContentRepository extends JpaRepository<AdContent, Long> {
    // 특정 adId에 해당하는 AdContent 엔티티를 Optional 형태로 찾는 메서드
    // AdContent는 각 AdId에 대해 유일하다고 가정합니다.
    Optional<AdContent> findByAdId(String adId);

    // 🔴 MetaAdCreatorService에서 사용하던 메서드 추가
    // 특정 userEmail로 생성된 AdContent 중 가장 최근(CreatedAt 기준)의 데이터를 하나 찾는 메서드
    Optional<AdContent> findFirstByUserEmailOrderByCreatedAtDesc(String userEmail);
}