package com.example.backend.controller;

import com.example.backend.service.AdInsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class AdInsightController {

    @Autowired
    private AdInsightService adInsightService;

    @GetMapping("/insight")
    public String getInsights(
            @RequestParam String adId,
            @RequestParam String accessToken) {

        adInsightService.fetchAndStoreInsights(adId, accessToken);
        return "✅ 광고 성과 저장 완료";
    }

    @GetMapping("/insight/test")
    public String testInsight() {
        adInsightService.fetchAndStoreInsights("test_ad_123", null);
        return "✅ 테스트용 성과 저장 완료";
    }
}
