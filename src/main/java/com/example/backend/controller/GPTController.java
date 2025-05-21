package com.example.backend.controller;

import com.example.backend.dto.PromptRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GPTController {

    @Value("${openai.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final MediaType mediaType = MediaType.parse("application/json");

    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody PromptRequest request) throws IOException {
        String prompt = String.format(
                "다음 제품 정보를 바탕으로 [%s] 톤의 광고 문구 2개를 작성해줘. 30자 이내로 CTA 포함.\n" +
                        "제품: %s\n응답 형식: [\"문구1\", \"문구2\"]",
                request.getStyle(),
                request.getProduct());

        System.out.println("GPT 프롬프트:\n" + prompt);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of("model", "gpt-4", "messages", List.of(message));
        String json = mapper.writeValueAsString(body);

        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(json, mediaType);
        Request gptRequest = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(gptRequest).execute()) {
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            String content = root.get("choices").get(0).get("message").get("content").asText();

            List<String> adTexts = mapper.readValue(content, List.class);

            System.out.println("✅ GPT 응답 문구들:");
            adTexts.forEach(System.out::println);

            return Map.of("adTexts", adTexts);
        }
    }
}
