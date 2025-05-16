package com.example.backend.controller;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.backend.dto.PromptRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GPTController {

    @Value("${openai.api.key}")
    private String apiKey;

    @PostMapping("/generate")
public Map<String, String> generate(@RequestBody PromptRequest request) throws IOException {
    String prompt = String.format(
        "다음 제품 정보를 바탕으로 [%s] 톤의 광고 문구 2개를 작성해줘. 30자 이내로 CTA 포함.\n" +
        "제품: %s\n응답 형식: [\"문구1\", \"문구2\"]",
        request.getStyle(),
        request.getProduct()
    );

    OkHttpClient client = new OkHttpClient();
    MediaType mediaType = MediaType.parse("application/json");

    ObjectMapper mapper = new ObjectMapper();

    Map<String, Object> message = Map.of(
        "role", "user",
        "content", prompt
    );
    Map<String, Object> bodyMap = Map.of(
        "model", "gpt-4",
        "messages", List.of(message)
    );

    String json = mapper.writeValueAsString(bodyMap);

    okhttp3.RequestBody body = okhttp3.RequestBody.create(json, mediaType);

    Request req = new Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .post(body)
        .addHeader("Authorization", "Bearer " + apiKey)
        .addHeader("Content-Type", "application/json")
        .build();

    try (Response response = client.newCall(req).execute()) {
        if (!response.isSuccessful()) {
        System.err.println("❌ GPT API 요청 실패: " + response.code() + " - " + response.message());
        }

        String result = response.body().string();
        return Map.of("output", result);
    }
}

}
