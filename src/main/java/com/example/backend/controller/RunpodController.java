package com.example.backend.controller;

import com.example.backend.dto.RunpodImageResponse;
import com.example.backend.service.RunpodImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runpod")
public class RunpodController {

    private final RunpodImageService runpodImageService;

    @PostMapping("/generate")
    public ResponseEntity<RunpodImageResponse> generateImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam("caption") String caption,
            @RequestParam(value = "product", required = false, defaultValue = "Product") String product,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(new RunpodImageResponse("ERROR: 로그인 후 이용해주세요."));
            }

            String email = userDetails.getUsername(); // 이메일 = 유저 식별자

            String base64Image = runpodImageService.generateImage(
                    caption, image, product, email);

            return ResponseEntity.ok(new RunpodImageResponse(base64Image));

        } catch (RuntimeException e) {
            // 예: 토큰 부족, 사용자 없음 등
            return ResponseEntity.status(403)
                    .body(new RunpodImageResponse("ERROR: " + e.getMessage()));
        } catch (Exception e) {
            // 기타 예외
            return ResponseEntity.internalServerError()
                    .body(new RunpodImageResponse("ERROR: " + e.getMessage()));
        }
    }
}
