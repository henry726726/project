package com.example.backend.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageGenerationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateImage(String caption, MultipartFile imageFile) throws IOException {
        String url = "http://localhost:8000/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // MultipartFile을 전송 가능한 형태로 래핑
        ByteArrayResource imageResource = new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename(); // 반드시 파일명 리턴!
            }
        };

        body.add("prompt", caption);
        body.add("image", imageResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // FastAPI 서버에 POST 요청
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        return (String) response.getBody().get("image_base64");
    }
}
