package com.example.backend.service;

import com.example.backend.dto.MetaAdCreationRequest;
import com.example.backend.entity.AccessTokenEntity;
import com.example.backend.entity.AdAccount;
import com.example.backend.entity.AdContent;
import com.example.backend.entity.User;
import com.example.backend.repository.AccessTokenRepository;
import com.example.backend.repository.AdAccountRepository;
import com.example.backend.repository.AdContentRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MetaAdCreatorService {

    @Autowired
    private AdAccountRepository adAccountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private AdContentRepository adContentRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void createInitialAd(MetaAdCreationRequest request) {
        User user = getCurrentUser();
        String userEmail = user.getEmail();

        // 콘텐츠 선택
        AdContent content;
        if (request.getContentId() != null) {
            Long id = Long.valueOf(request.getContentId());
            content = adContentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("❌ 콘텐츠가 존재하지 않습니다"));
        } else {
            content = adContentRepository.findFirstByUserEmailOrderByCreatedAtDesc(userEmail)
                    .orElseThrow(() -> new RuntimeException("❌ 해당 유저의 최신 콘텐츠가 없습니다"));
        }

        String caption = content.getAdText();
        String imageBase64 = content.getGeneratedImageBase64();

        // 액세스 토큰 조회
        AccessTokenEntity tokenEntity = accessTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("❌ AccessToken이 존재하지 않습니다"));
        String accessToken = tokenEntity.getAccessToken();

        // ✅ 프론트에서 전달받은 광고 계정 ID / 페이지 ID 사용
        String adAccountId = normalizeActId(request.getAccountId());
        String pageId = request.getPageId();
        if (pageId == null || pageId.isBlank()) {
            throw new RuntimeException("❌ 해당 광고 계정에 연결된 페이지 ID가 없습니다");
        }

        // 이미지 업로드
        String imageHash = uploadImageToFacebook(adAccountId, accessToken, imageBase64);

        // 캠페인 생성
        String campaignId = createCampaign(adAccountId, accessToken);

        // 광고 세트 생성
        String adSetId = createAdSet(
                adAccountId, campaignId, accessToken,
                request.getBillingEvent(),
                request.getOptimizationGoal(),
                request.getBidStrategy(),
                request.getDailyBudget(),
                request.getStartTime());

        // 광고 크리에이티브 생성
        String creativeId = createAdCreative(adAccountId, accessToken, caption, imageHash, pageId, request.getLink());

        // 광고 생성
        createAd(adAccountId, adSetId, creativeId, accessToken);
    }

    // ==== 유틸 ====

    private String normalizeActId(String adAccountId) {
        if (adAccountId == null)
            throw new IllegalArgumentException("adAccountId is null");
        String trimmed = adAccountId.trim();
        return trimmed.startsWith("act_") ? trimmed : "act_" + trimmed;
    }

    private AdAccount getAdAccountByUser(User user) {
        return adAccountRepository.findByUser(user).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("❌ 유저에 해당하는 광고 계정을 찾을 수 없습니다"));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("❌ 현재 로그인한 유저를 찾을 수 없습니다"));
    }

    // ==== Graph API ====

    private String createCampaign(String adAccountId, String accessToken) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/campaigns";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Campaign");
        body.add("objective", "OUTCOME_TRAFFIC");
        body.add("status", "PAUSED");
        body.add("access_token", accessToken);
        body.add("special_ad_categories", "[\"NONE\"]");
        return postAndExtractId(url, body);
    }

    private String createAdSet(
            String adAccountId, String campaignId, String accessToken,
            String billingEvent, String optimizationGoal, String bidStrategy,
            String dailyBudget, String startTime) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/adsets";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New AdSet");
        body.add("campaign_id", campaignId);
        body.add("billing_event", billingEvent);
        body.add("optimization_goal", optimizationGoal);
        body.add("bid_strategy", bidStrategy);
        body.add("daily_budget", dailyBudget);

        String startTimeEpoch = (startTime != null && !startTime.isBlank())
                ? String.valueOf(Instant.parse(startTime + ":00Z").getEpochSecond())
                : String.valueOf(Instant.now().plus(1, ChronoUnit.MINUTES).getEpochSecond());

        body.add("start_time", startTimeEpoch);
        body.add("targeting", getFacebookTargetingJson());
        body.add("status", "PAUSED");
        body.add("access_token", accessToken);
        return postAndExtractId(url, body);
    }

    private String createAdCreative(String adAccountId, String accessToken, String caption, String imageHash,
            String pageId, String link) {

        if (link == null || link.isBlank()) {
            throw new RuntimeException("❌ 랜딩 URL(link)은 필수입니다.");
        }

        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/adcreatives";
        String objectStorySpecJson = String.format(
                "{\"page_id\":\"%s\",\"link_data\":{\"message\":\"%s\",\"image_hash\":\"%s\",\"link\":\"%s\"}}",
                pageId, escapeForJson(caption), imageHash, escapeForJson(link));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Creative");
        body.add("object_story_spec", objectStorySpecJson);
        body.add("access_token", accessToken);
        return postAndExtractId(url, body);
    }

    private void createAd(String adAccountId, String adSetId, String creativeId, String accessToken) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/ads";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Ad");
        body.add("adset_id", adSetId);
        body.add("creative", String.format("{\"creative_id\":\"%s\"}", creativeId));
        body.add("status", "PAUSED");
        body.add("access_token", accessToken);
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, getFormHeaders()),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("❌ Ad 생성 실패: " + response.getBody());
        }
    }

    private String uploadImageToFacebook(String adAccountId, String accessToken, String imageBase64) {
        // 1) data URL 프리픽스/공백 제거
        String cleaned = imageBase64 != null ? imageBase64 : "";
        int comma = cleaned.indexOf(',');
        if (comma >= 0)
            cleaned = cleaned.substring(comma + 1);
        cleaned = cleaned.replaceAll("\\s+", "");

        // 2) 디코드
        byte[] imageBytes = Base64.getDecoder().decode(cleaned);

        // 3) 이미지 포맷 강제 변환 (jpg)
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null)
                throw new IllegalArgumentException("이미지 디코딩 실패");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            imageBytes = baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("이미지 포맷 변환 실패", e);
        }

        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/adimages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("access_token", accessToken);
        body.add("filename", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "ad_image.jpg"; // 파일명 필수
            }
        });

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers),
                String.class);

        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            // images -> { "ad_image.jpg": { "hash": "..." } }
            String hash = json.path("images").path("ad_image.jpg").path("hash").asText();
            if (hash == null || hash.isBlank()) {
                throw new RuntimeException("이미지 해시 파싱 실패: " + response.getBody());
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException("❌ 이미지 업로드 실패: " + response.getBody(), e);
        }
    }

    // ==== 공통 ====

    private HttpHeaders getFormHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private String getFacebookTargetingJson() {
        Map<String, Object> targeting = new HashMap<>();
        targeting.put("geo_locations", Collections.singletonMap("countries", Collections.singletonList("KR")));
        targeting.put("publisher_platforms", List.of("facebook"));
        targeting.put("facebook_positions", List.of("feed", "right_hand_column"));
        try {
            return objectMapper.writeValueAsString(targeting);
        } catch (Exception e) {
            throw new RuntimeException("❌ 타겟팅 JSON 생성 실패", e);
        }
    }

    private String postAndExtractId(String url, MultiValueMap<String, String> body) {
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, getFormHeaders()),
                String.class);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            String id = json.path("id").asText();
            if (id == null || id.isBlank()) {
                throw new RuntimeException("ID 파싱 실패: " + response.getBody());
            }
            return id;
        } catch (Exception e) {
            throw new RuntimeException("❌ ID 파싱 실패: " + response.getBody(), e);
        }
    }

    private String escapeForJson(String s) {
        if (s == null)
            return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
