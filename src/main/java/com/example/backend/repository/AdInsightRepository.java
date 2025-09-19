package com.example.backend.repository;

import com.example.backend.entity.AdInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate; // LocalDate를 사용하므로 임포트해야 합니다.
import java.util.List;
import java.util.Optional;

public interface AdInsightRepository extends JpaRepository<AdInsight, Long> {

    // 광고 ID 하나에 해당하는 인사이트 리스트 조회
    List<AdInsight> findAllByAdId(String adId);

    // 복수 광고 ID 리스트에 해당하는 인사이트 조회
    List<AdInsight> findByAdIdIn(List<String> adIds);

    // 중복 저장 방지를 위한 검사 메서드:
    // 특정 광고 ID와 특정 날짜의 데이터가 이미 존재하는지 확인하는 메서드
    boolean existsByAdIdAndDate(String adId, LocalDate date);

    // 🔴 중요! ReportService에서 사용하고 있던 바로 그 메서드! 이걸 추가해 줘야 해!
    List<AdInsight> findAllByAdIdAndDate(String adId, LocalDate date);


    // (필요시 추가: 특정 기간의 AdInsight를 가져오는 메서드)
    // List<AdInsight> findAllByAdIdAndDateBetween(String adId, LocalDate startDate, LocalDate endDate);

}