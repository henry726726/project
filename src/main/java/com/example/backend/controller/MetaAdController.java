package com.example.backend.controller;

import com.example.backend.service.MetaAdService;
import com.example.backend.meta.MetaAdUpdater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdController {

    @Autowired
    private MetaAdService metaAdService;

    @Autowired
    private MetaAdUpdater metaAdUpdater;

    // ✅ 테스트용 access token 저장 (user 연결 없음)
    @GetMapping("/adaccounts/testsave")
    public ResponseEntity<?> saveWithToken(@RequestParam String token) {
        metaAdService.saveAdAccountsWithoutUser(token);
        return ResponseEntity.ok("✅ 테스트 저장 완료");
    }

    // ✅ 로그인 사용자 기준 저장
    @GetMapping("/adaccounts/save")
    public ResponseEntity<?> saveWithLoginUser(@RequestParam String accessToken) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        metaAdService.saveAdAccountsForUser(accessToken, email);
        return ResponseEntity.ok("✅ 사용자 기반 저장 완료");
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
