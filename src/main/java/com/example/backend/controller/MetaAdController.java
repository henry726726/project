package com.example.backend.controller;

import com.example.backend.service.MetaAdService;
import com.example.backend.meta.MetaAdUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdController {

    @Autowired
    private MetaAdService metaAdService;
    @Autowired
    private MetaAdUpdater metaAdUpdater;

    // ✅ 테스트용 access token 직접 호출
    @GetMapping("/adaccounts/testsave")
    public ResponseEntity<?> saveWithToken(@RequestParam String token) {
        metaAdService.saveAdAccounts(token);
        return ResponseEntity.ok("Saved via token.");
    }

    // ✅ 광고 업데이트 테스트용
    @GetMapping("/test-update")
    public ResponseEntity<String> testUpdate(
            @RequestParam String contentId,
            @RequestParam String userId) {
        metaAdUpdater.updateAdByContentId(contentId, userId);
        return ResponseEntity.ok("광고 업데이트 완료");
    }
}
