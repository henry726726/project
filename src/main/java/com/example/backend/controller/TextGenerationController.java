package com.example.backend.controller; // ✅ 이 줄이 정상적으로 있어야 함.

import com.example.backend.dto.TextGenerationRequest;
import com.example.backend.dto.TextGenerationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException; // ✅ HttpClientErrorException 임포트 (이전에 추가했음)
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays; // Arrays.asList 대신 List.of 사용 시 불필요. 그래도 안전하게 유지.

@RestController
@RequestMapping("/api")
public class TextGenerationController {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ ObjectMapper 인스턴스 생성

    @PostMapping("/generate")
    public ResponseEntity<TextGenerationResponse> generateAdText(@RequestBody TextGenerationRequest request) {
        String openaiUrl = "https://api.openai.com/v1/chat/completions";

        String prompt = String.format(
                "제품명: %s, 타겟: %s, 목적: %s, 강조 키워드: %s, 광고 기간: %s.\n" +
                        "위 정보를 바탕으로 창의적이고 설득력 있는 광고 문구 3개를 30자 내외로 생성해줘. 각 문구는 줄바꿈으로 구분해줘.",
                request.getProduct(),
                request.getTarget(),
                request.getPurpose(),
                request.getKeyword(),
                request.getDuration());

        String requestBody = String.format(
                "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 300, \"temperature\": 0.7}",
                prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(openaiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String responseBody = response.getBody();
                System.out.println("DEBUG: Full OpenAI Response Body: " + responseBody);

                // ✅ 핵심 변경: ObjectMapper를 사용하여 JSON 응답을 안전하게 파싱
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String generatedContent = rootNode.path("choices").path(0).path("message").path("content").asText();

                // 줄바꿈 기준으로 문구 분리
                // OpenAI 응답의 content에는 \n (이스케이프되지 않은 실제 줄바꿈 문자)가 있으므로 \n으로 분리
                List<String> adTexts = Arrays.stream(generatedContent.split("\\n")) // ✅ 줄바꿈 문자로 분리
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                // 생성된 문구가 없거나 비어있는 경우 (파싱은 성공했으나 결과가 없는 경우)
                if (adTexts.isEmpty()) {
                    return ResponseEntity
                            .ok(new TextGenerationResponse(Collections.singletonList("OpenAI 응답은 받았으나 생성된 문구가 없습니다.")));
                }

                return ResponseEntity.ok(new TextGenerationResponse(adTexts));
            } else {
                System.err.println("OpenAI API 호출 실패: HTTP Status " + response.getStatusCode() + ", Response: "
                        + response.getBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new TextGenerationResponse(
                                Collections.singletonList("문구 생성 중 오류 발생 (OpenAI API 응답 실패)")));
            }
        } catch (HttpClientErrorException.Unauthorized e) { // 401 Unauthorized를 직접 캐치
            System.err.println("OpenAI API 호출 중 예외 발생: 401 Unauthorized - API 키 오류 예상");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 클라이언트(Postman)에게 401을 돌려줌
                    .body(new TextGenerationResponse(Collections.singletonList("OpenAI API 키가 유효하지 않습니다. 확인해주세요.")));
        } catch (HttpClientErrorException e) { // 다른 HttpClientErrorException (4xx 에러)
            System.err.println("OpenAI API 호출 중 클라이언트 에러 발생: " + e.getStatusCode() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(e.getStatusCode()) // 받은 상태 코드를 그대로 돌려줌
                    .body(new TextGenerationResponse(
                            Collections.singletonList("OpenAI API 호출 중 오류 발생: " + e.getStatusCode())));
        } catch (Exception e) { // 기타 모든 예외
            System.err.println("OpenAI API 호출 중 예상치 못한 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TextGenerationResponse(
                            Collections.singletonList("문구 생성 중 알 수 없는 오류가 발생했습니다.")));
        }
    }
}