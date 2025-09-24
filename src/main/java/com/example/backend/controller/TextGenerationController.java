package com.example.backend.controller;

import com.example.backend.dto.PromptRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TextGenerationController {

        @Value("${openai.api.key}")
        private String apiKey;

        private final OkHttpClient client = new OkHttpClient();
        private final ObjectMapper mapper = new ObjectMapper();
        private final MediaType mediaType = MediaType.parse("application/json");

        @PostMapping("/generate")
        public Map<String, Object> generate(@RequestBody PromptRequest request) throws IOException {
                // 🔹 확장된 프롬프트 템플릿
                String prompt = String.format(
                                """               
                                아래 광고 정보를 바탕으로, 실제 온라인 광고에 쓸 수 있는 초단문 카피 3개를 만든다. 
                                각 문구는 공백 포함 30자 이내, 한국어.
                                서로 다른 접근 3종으로 작성: ①혜택형 ②구매유도형 ③사회적증거/신뢰형.
                                과장/허위는 금지. 입력의 금지어는 사용하지 않는다.
                                이모지/해시태그/따옴표/말줄임표(...) 금지. 특수문자 최소화, 마침표 생략.
                                브랜드명은 최대 1회만 노출.
                                세 문구 간 중복 어휘 최소화.
                                30자 초과가 하나라도 있으면 전부 재작성하여 모두 30자 이내로 맞춘다.\n\n"
                                                +
                                                "제품명: %s\n" +
                                                "타겟: %s\n" +
                                                "목적: %s\n" +
                                                "강조 키워드: %s\n" +
                                                "광고 기간: %s\n\n" +
                                                "응답 형식: [\"문구1\", \"문구2\", \"문구3\"]""",

                                request.getProduct(),
                                request.getTarget(),
                                request.getPurpose(),
                                request.getKeyword(),
                                request.getDuration());

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

                        // 👉 응답 구조가 에러인지 먼저 확인
                        if (root.has("error")) {
                                String errorMessage = root.get("error").get("message").asText();
                                throw new RuntimeException("OpenAI API 오류: " + errorMessage);
                        }

                        // 👉 안전하게 choices 추출
                        JsonNode choicesNode = root.get("choices");
                        if (choicesNode == null || !choicesNode.isArray() || choicesNode.isEmpty()) {
                                throw new RuntimeException("OpenAI 응답에 choices가 없습니다: " + responseBody);
                        }

                        JsonNode messageNode = choicesNode.get(0).get("message");
                        if (messageNode == null || messageNode.get("content") == null) {
                                throw new RuntimeException("OpenAI 응답에 content가 없습니다: " + responseBody);
                        }

                        String content = messageNode.get("content").asText();

                        // 👉 GPT가 문자열 배열 형식으로 응답 안 주는 경우 대비 (ex. 그냥 문자열로 응답하는 경우)
                        List<String> adTexts;
                        try {
                                adTexts = mapper.readValue(content, List.class);
                        } catch (Exception e) {
                                throw new RuntimeException("GPT 응답이 올바른 JSON 배열 형식이 아님:\n" + content);
                        }

                        System.out.println("✅ GPT 응답 문구들:");
                        adTexts.forEach(System.out::println);

                        return Map.of("adTexts", adTexts);
                }

        }
}