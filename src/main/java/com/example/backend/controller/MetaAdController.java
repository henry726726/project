package com.example.backend.controller;

import com.example.backend.dto.CreateAdRequest; // 위에서 만든 DTO 임포트
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta") // 기본 경로 /api/meta
public class MetaAdController {

    // ✅ 여기에 MetaAdsService 주입 및 로직 추가 예정
    // private final MetaAdsService metaAdsService;

    // POST /api/meta/create-ad 요청을 처리할 메서드
    @PostMapping("/create-ad")
    public ResponseEntity<String> createAd(@RequestBody CreateAdRequest request) {
        System.out.println("광고 생성 요청 수신!");
        System.out.println("  Billing Event: " + request.getBillingEvent());
        System.out.println("  Optimization Goal: " + request.getOptimizationGoal());
        System.out.println("  Bid Strategy: " + request.getBidStrategy());
        System.out.println("  Daily Budget: " + request.getDailyBudget());
        System.out.println("  Start Time: " + request.getStartTime());

        // ✅ 실제 Meta API 호출 로직은 여기에 들어갑니다. (나중에 MetaAdsService로 분리)
        // 현재는 더미 응답을 반환하여 404 에러를 없애는 것이 목표!

        return ResponseEntity.ok("광고 생성 요청이 성공적으로 수신되었습니다! (실제 생성 로직 미구현)");
    }
}