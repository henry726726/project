package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service
public class RunpodImageService {

    @Value("${runpod.api.key}")
    private String runpodApiKey;

    @Value("${runpod.endpoint.id}")
    private String runpodEndpointId;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository userRepository;

    public RunpodImageService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * RunPod 이미지 생성 + 토큰 차감
     */
    @Transactional
    public String generateImage(
            String caption,
            MultipartFile imageFile,
            String productName,
            String userEmail) throws Exception {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
        }

        // ===============================
        // 1️⃣ 사용자 조회 + 토큰 확인
        // ===============================
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getImageToken() <= 0) {
            throw new RuntimeException("이미지 생성 토큰이 부족합니다.");
        }

        // 👉 먼저 차감 (트랜잭션이므로 실패 시 롤백됨)
        user.setImageToken(user.getImageToken() - 1);
        userRepository.save(user);

        // ===============================
        // 2️⃣ 이미지 → Base64
        // ===============================
        String base64Image = toBase64WithPrefix(imageFile);

        // ===============================
        // 3️⃣ RunPod 요청
        // ===============================
        String runUrl = "https://api.runpod.ai/v2/" + runpodEndpointId + "/run";
        String statusUrl = "https://api.runpod.ai/v2/" + runpodEndpointId + "/status/";

        ObjectNode payload = mapper.createObjectNode();
        ObjectNode input = payload.putObject("input");
        input.put("image", base64Image);
        input.put("product_name", productName);
        input.put("headline", caption);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(runUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + runpodApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> runResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (runResponse.statusCode() != 200) {
            throw new RuntimeException("RunPod run API 호출 실패");
        }

        String jobId = mapper.readTree(runResponse.body()).get("id").asText();

        // ===============================
        // 4️⃣ 결과 Polling
        // ===============================
        String finalImageBase64 = null;

        for (int i = 0; i < 30; i++) {
            Thread.sleep(2000);

            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl + jobId))
                    .header("Authorization", "Bearer " + runpodApiKey)
                    .build();

            HttpResponse<String> statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());

            JsonNode statusJson = mapper.readTree(statusResponse.body());
            String status = statusJson.get("status").asText();

            if ("COMPLETED".equals(status)) {
                finalImageBase64 = statusJson.path("output").path("image").asText();
                break;
            }

            if ("FAILED".equals(status)) {
                throw new RuntimeException("RunPod 작업 실패");
            }
        }

        if (finalImageBase64 == null || finalImageBase64.isBlank()) {
            throw new RuntimeException("이미지 생성 실패 또는 시간 초과");
        }

        return finalImageBase64;
    }

    private String toBase64WithPrefix(MultipartFile file) throws IOException {
        String mimeType = file.getContentType();
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + mimeType + ";base64," + base64;
    }
}
