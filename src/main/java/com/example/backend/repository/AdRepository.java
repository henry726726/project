package com.example.backend.repository;

import java.util.List;
import java.util.Optional; // AdAccount 엔티티가 Ad와 연관되어 있으므로 임포트

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Ad; // List 임포트 추가
import com.example.backend.entity.AdAccount; // Optional 임포트 추가 (existsByAdId 등에서 사용될 수 있으므로)

public interface AdRepository extends JpaRepository<Ad, Long> {

    // 특정 adId (Meta에서 가져온 광고 ID)를 가진 Ad 엔티티가 존재하는지 확인하는 메서드
    // AdSyncService에서 중복 저장을 방지할 때 사용됩니다.
    boolean existsByAdId(String adId);

    // 특정 AdAccount에 연결된 모든 Ad 엔티티를 찾는 메서드
    // 스케줄러에서 해당 광고 계정의 모든 광고를 가져와 성과를 수집할 때 사용됩니다.
    List<Ad> findByAdAccount(AdAccount adAccount); // 🔴 이 부분이 스케줄러를 위해 추가된 부분!

    // (기존에 있을 수 있는 다른 메서드들 - 예시)
    Optional<Ad> findByAdId(String adId);
}