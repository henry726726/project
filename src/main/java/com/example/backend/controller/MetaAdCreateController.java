package com.example.backend.controller;

import com.example.backend.service.MetaAdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdCreateController {

    @Autowired
    private MetaAdService metaAdService;

    // 👉 비즈니스 포트폴리오 광고 계정만 저장하는 엔드포인트
    @GetMapping("/adaccounts/business")
    public ResponseEntity<String> saveBusinessAdAccounts(@RequestParam String token) {
        metaAdService.saveAdAccounts(token); // ✅ 기존 메서드 호출
        return ResponseEntity.ok("✅ 비즈니스 광고 계정 저장 완료");
    }
}
