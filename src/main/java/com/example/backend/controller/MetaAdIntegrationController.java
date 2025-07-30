// src/main/java/com/example/backend/controller/MetaAdIntegrationController.java
package com.example.backend.controller;

import com.example.backend.dto.AdAccountDto; // 💡 AdAccountDto 임포트
import com.example.backend.meta.MetaAdUpdater;
import com.example.backend.service.AdSyncService; // AdSyncService 임포트가 없었다면 추가
import com.example.backend.service.MetaAdCreatorService; // 💡 MetaAdCreatorService 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta-ads")
@RequiredArgsConstructor
public class MetaAdIntegrationController {

    private final MetaAdCreatorService metaAdCreatorService; // 💡 MetaAdCreatorService로 변경
    private final MetaAdUpdater metaAdUpdater;
    private final AdSyncService adSyncService; // AdSyncService 필드가 없었다면 추가

    // 생성자 (롬복의 @RequiredArgsConstructor로 대체 가능)
    // public MetaAdIntegrationController(MetaAdCreatorService metaAdCreatorService, MetaAdUpdater metaAdUpdater, AdSyncService adSyncService) {
    //     this.metaAdCreatorService = metaAdCreatorService;
    //     this.metaAdUpdater = metaAdUpdater;
    //     this.adSyncService = adSyncService;
    // }

    @PostMapping("/save-account")
    public ResponseEntity<?> saveMetaAdAccount(@RequestBody AdAccountDto adAccountDto) { // 💡 AdAccountDto 사용
        // 사용자 이메일은 인증 정보에서 가져오는 로직 필요
        String userEmail = "testuser@example.com"; // 💡 임시 사용자 이메일 (인증 구현 시 실제 사용자 정보 사용)
        metaAdCreatorService.saveAdAccount(userEmail, adAccountDto);
        return ResponseEntity.ok("Meta Ad Account saved/updated successfully!");
    }

    @PostMapping("/update-ads")
    public ResponseEntity<?> updateMetaAds(@RequestBody AdAccountDto adAccountDto) { // 💡 AdAccountDto 사용
        // 사용자 이메일은 인증 정보에서 가져오는 로직 필요
        String userEmail = "testuser@example.com"; // 💡 임시 사용자 이메일 (인증 구현 시 실제 사용자 정보 사용)
        metaAdUpdater.updateAdAccount(userEmail, adAccountDto.getAccessToken());
        return ResponseEntity.ok("Meta Ads updated successfully!");
    }
}