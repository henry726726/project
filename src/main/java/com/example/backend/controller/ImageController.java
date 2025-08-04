package com.example.backend.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.service.ImageGenerationService;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageGenerationService imageGenerationService;

    public ImageController(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping("/generate-image")
    public ResponseEntity<String> generateImage(
            @RequestParam("caption") String caption,
            @RequestParam("image") MultipartFile image) throws IOException {
        String base64Image = imageGenerationService.generateImage(caption, image);
        return ResponseEntity.ok(base64Image);
    }
}