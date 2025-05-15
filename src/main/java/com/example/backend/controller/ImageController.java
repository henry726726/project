package com.example.backend.controller;

import com.example.backend.dto.ImageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageController {

    @Value("${openai.api.key}")
    private String apiKey;

    // ✅ 타임아웃 설정된 OkHttpClient
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaType mediaType = MediaType.parse("application/json");

    @PostMapping("/image")
    public Map<String, String> generateImage(@RequestBody ImageRequest request) throws IOException {
        String dallePrompt = String.format(
            "%s 제품 광고 이미지. '%s' 문구가 이미지에 포함되어야 함.",
            request.getProduct(), request.getText()
        );

        System.out.println("🟢 DALL·E 프롬프트: " + dallePrompt);

        Map<String, Object> body = Map.of(
            "prompt", dallePrompt,
            "n", 1,
            "size", "1024x1024"
        );

        String json = mapper.writeValueAsString(body);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(json, mediaType);

        Request dalleRequest = new Request.Builder()
            .url("https://api.openai.com/v1/images/generations")
            .post(requestBody)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(dalleRequest).execute()) {
            String responseBody = response.body().string();
            System.out.println("🟢 DALL·E 응답: " + responseBody);

            JsonNode root = mapper.readTree(responseBody);
            String imageUrl = root.get("data").get(0).get("url").asText();

            return Map.of("imageUrl", imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 생성 실패: " + e.getMessage());
        }
    }
}
