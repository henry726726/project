// src/main/java/com/example/backend/controller/ImageController.java
package com.example.backend.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.example.backend.service.ImageGenerationService;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationService imageGenerationService;

    // 기존의 "/generate-image"가 Base64를 리턴하던 메서드는 삭제/주석 처리하고,
    // 아래 메서드로 교체하세요. (동일한 URL이므로 충돌 방지)
    @PostMapping(
        value = "/generate-image",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateImage(
            @RequestParam("caption") String caption,
            @RequestParam("image") MultipartFile image,
            @RequestParam("userEmail") String userEmail
    ) throws Exception {
        Long id = imageGenerationService.generateAndSave(caption, image);
        return ResponseEntity.ok(Map.of("adContentId", id, "message", "saved"));
    }
}
