// src/main/java/com/example/backend/controller/AdAndImageProcessingController.java
package com.example.backend.controller;

import com.example.backend.dto.AdGenerationParameters; // 💡 AdGenerationParameters 임포트
import com.example.backend.dto.TextGenerationResponse; // 💡 TextGenerationResponse 임포트
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api") // /api 경로로 시작
public class AdAndImageProcessingController {

    @PostMapping("/generate")
    public ResponseEntity<TextGenerationResponse> generateAdText(@RequestBody AdGenerationParameters request) { // 💡 DTO 사용
        // 이 곳에서 OpenAI API 등을 호출하여 광고 문구를 생성합니다.
        // 현재는 더미 데이터 반환
        String generatedText1 = String.format("프리미엄 %s로 %s %s을 유도하세요! (%s, %s)",
                request.getProduct(), request.getTarget(), request.getPurpose(),
                request.getKeyword(), request.getDuration());
        String generatedText2 = String.format("놀라운 %s! %s의 마음을 사로잡으세요! (%s)",
                request.getProduct(), request.getTarget(), request.getKeyword());
        String generatedText3 = String.format("%s와 함께 %s 동안 최고의 %s을 경험하세요!",
                request.getKeyword(), request.getDuration(), request.getProduct());

        List<String> adTexts = List.of(generatedText1, generatedText2, generatedText3);

        // 💡💡💡 생성자에 List<String>을 넘깁니다!
        return ResponseEntity.ok(new TextGenerationResponse(adTexts));
    }

    // 다른 광고 및 이미지 처리 API들이 여기에 추가될 수 있습니다.
    // 예: 이미지 생성 API, 광고 콘텐츠 저장 API 등
}