package com.example.backend.repository;

import com.example.backend.entity.Ad;
import com.example.backend.entity.AdAccount;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdRepository extends JpaRepository<Ad, String> {
    boolean existsByAdId(String adId);

    List<Ad> findByAdAccount(AdAccount adAccount); // 🔴 이 부분이 스케줄러를 위해 추가된 부분!

    // (기존에 있을 수 있는 다른 메서드들 - 예시)
    Optional<Ad> findByAdId(String adId);
}
