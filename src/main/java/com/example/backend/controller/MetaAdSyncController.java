package com.example.backend.controller;

import com.example.backend.service.AdSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdSyncController {

    @Autowired
    private AdSyncService adSyncService;

    @GetMapping("/sync-ads")
    public String syncAds(
            @RequestParam String adAccountId,
            @RequestParam String accessToken) {
        adSyncService.syncAdsFromMeta(adAccountId, accessToken);
        return "✅ 광고 정보 동기화 완료 (DB 저장됨)";
    }
}
