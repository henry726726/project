package com.example.backend.controller;

import com.example.backend.dto.SaveAdContentRequest;
import com.example.backend.entity.AdContent;
import com.example.backend.service.AdContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 광고 콘텐츠 저장 API를 처리하는 컨트롤러
@RestController
@RequestMapping("/api/ad-content") // 기본 경로 /api/ad-content
@RequiredArgsConstructor // AdContentService 의존성 주입
public class AdContentController {

    private final AdContentService adContentService; // AdContentService 주입

    // POST /api/ad-content/save 요청을 처리할 메서드
    @PostMapping("/save")
    public ResponseEntity<String> saveAdContent(@RequestBody SaveAdContentRequest request) {
        try {
            // Service 계층에 데이터 저장 요청 위임
            AdContent savedContent = adContentService.saveAdContent(request);

            System.out.println("광고 콘텐츠 저장 성공! ID: " + savedContent.getId());
            return ResponseEntity.ok("광고 콘텐츠가 성공적으로 저장되었습니다. ID: " + savedContent.getId());
        } catch (Exception e) {
            System.err.println("광고 콘텐츠 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("광고 콘텐츠 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}