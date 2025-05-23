package com.example.demo.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");

    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<?> registerProduct(
            @RequestPart("product") @Valid ProductRequest product,
            @RequestPart("image") MultipartFile imageFile
    ) {
        // 파일 유효성 검증
        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body("이미지 파일이 없습니다.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(imageFile.getContentType())) {
            return ResponseEntity.badRequest().body("지원하지 않는 이미지 형식입니다. (JPEG, PNG만 허용)");
        }

        if (imageFile.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("파일 크기는 50MB 이하만 허용됩니다.");
        }

        // 저장 로직 (DB + 이미지 저장 등)
        // product.getName(), product.getDescription(), imageFile → Service에 전달

        return ResponseEntity.ok("상품 등록 완료");
    }
}
