package com.example.backend.controller;

import com.example.backend.service.MetaAdCreatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdUpdateController {

    @Autowired
    private MetaAdCreatorService metaAdUpdaterService;

    /**
     * 광고 업데이트 테스트용 엔드포인트
     * 
     * @param adRunId      기존 광고 실행(AdRun) ID
     * @param newContentId 교체할 콘텐츠 ID
     * @param userEmail    유저 이메일
     */
    @PostMapping("/update-ad")
    public ResponseEntity<String> updateAd(
            @RequestParam Long adRunId,
            @RequestParam Long newContentId,
            @RequestParam String userEmail) {
        try {
            metaAdUpdaterService.updateAd(adRunId, newContentId, userEmail);
            return ResponseEntity.ok("✅ 광고 업데이트 완료");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ 광고 업데이트 실패: " + e.getMessage());
        }
    }
}
