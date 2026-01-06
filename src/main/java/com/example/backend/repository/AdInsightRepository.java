package com.example.backend.repository;

import com.example.backend.entity.AdInsight;
import com.example.backend.entity.AdRun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface AdInsightRepository extends JpaRepository<AdInsight, Long> {
    Optional<AdInsight> findTopByAdIdOrderByDateDesc(String adId);

    // 광고 ID 하나에 해당하는 인사이트 리스트 조회
    List<AdInsight> findAllByAdId(String adId);

    // 복수 광고 ID 리스트에 해당하는 인사이트 조회
    List<AdInsight> findByAdIdIn(List<String> adIds);

    // 중복 저장 방지를 위한 검사 메서드:
    // 특정 광고 ID와 특정 날짜의 데이터가 이미 존재하는지 확인하는 메서드
    boolean existsByAdIdAndDate(String adId, LocalDate date);

    // 🔴 중요! ReportService에서 사용하고 있던 바로 그 메서드! 이걸 추가해 줘야 해!
    List<AdInsight> findAllByAdIdAndDate(String adId, LocalDate date);

    Optional<AdInsight> findTopByAdRunOrderByDateDesc(AdRun adRun);

    List<AdInsight> findAllByAdRun(AdRun adRun);

    boolean existsByAdRunAndDate(AdRun adRun, LocalDate date);

    Optional<AdInsight> findTopByAdRun_IdOrderByDateDesc(Long adRunId);

}
