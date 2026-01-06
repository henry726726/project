package com.example.backend.controller;

import com.example.backend.entity.AdInsight;
import com.example.backend.service.AdInsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/meta/insights")
public class AdInsightController {

    private final AdInsightService adInsightService;

    public AdInsightController(AdInsightService adInsightService) {
        this.adInsightService = adInsightService;
    }

    /**
     * ✅ 특정 AdRun의 성과 수집 & 저장
     */
    @PostMapping("/fetch-and-save")
    public ResponseEntity<String> fetchAndSave(@RequestBody Map<String, Object> body) {
        Long adRunId = Long.valueOf(body.get("adRunId").toString());

        try {
            adInsightService.fetchAndStoreInsightsByAdRunId(adRunId);
            return ResponseEntity.ok("✅ 성과 저장 완료 (adRunId=" + adRunId + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("❌ 성과 저장 실패: " + e.getMessage());
        }
    }

    /**
     * ✅ 특정 AdRun의 최신 성과 조회
     */
    @GetMapping("/{adRunId}/latest")
    public ResponseEntity<AdInsight> getLatestInsight(@PathVariable Long adRunId) {
        AdInsight latest = adInsightService.getLatestInsightByAdRunId(adRunId);
        return ResponseEntity.ok(latest);
    }
}
