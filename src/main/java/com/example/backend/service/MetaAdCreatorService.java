package com.example.backend.service;

import com.example.backend.entity.AccessTokenEntity;
import com.example.backend.entity.AdAccount;
import com.example.backend.entity.Content;
import com.example.backend.repository.AccessTokenRepository;
import com.example.backend.repository.AdAccountRepository;
import com.example.backend.repository.ContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class MetaAdCreatorService {

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private AdAccountRepository adAccountRepository;

    @Autowired
    private ContentRepository contentRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void createInitialAdByContentId(String contentId, String placementOption) {

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("콘텐츠 없음"));

        String userId = content.getUserdatainput().getId();
        String caption = content.getCaption();
        String imageUrl = content.getImageUrl();

        String accessToken = getAccessToken(userId);

        String adAccountId = getAdAccountId();

        String campaignId = createCampaign(adAccountId, accessToken);
        String adSetId = createAdSet(adAccountId, campaignId, accessToken, placementOption);
        String creativeId = createAdCreative(adAccountId, accessToken, caption, imageUrl);

        createAd(adAccountId, adSetId, creativeId, accessToken);
    }

    private String getAccessToken(String userId) {
        return accessTokenRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No access token for user: " + userId))
                .getAccessToken();
    }

    private String getAdAccountId() {
        String rawAccountId = adAccountRepository.findAll().stream()
                .findFirst().orElseThrow(() -> new RuntimeException("No ad account found"))
                .getAccountId();
        return "act_" + rawAccountId;
    }

    private String createCampaign(String adAccountId, String accessToken) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/campaigns";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Campaign2");
        body.add("objective", "OUTCOME_TRAFFIC");
        body.add("status", "PAUSED");
        body.add("access_token", accessToken);
        body.add("special_ad_categories", "[\"NONE\"]");

        return postAndExtractId(url, body);
    }

    private String createAdSet(String adAccountId, String campaignId, String accessToken, String placementOption) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/adsets";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New AdSet_2");
        body.add("campaign_id", campaignId);
        body.add("billing_event", "IMPRESSIONS");
        body.add("optimization_goal", "LINK_CLICKS");
        body.add("bid_strategy", "LOWEST_COST_WITHOUT_CAP");
        body.add("daily_budget", "140000");
        body.add("start_time", String.valueOf(Instant.now().plus(1, ChronoUnit.MINUTES).getEpochSecond()));

        String targetingJson = getTargetingJson(placementOption);
        body.add("targeting", targetingJson);

        body.add("status", "PAUSED");
        body.add("access_token", accessToken);

        return postAndExtractId(url, body);
    }

    private String createAdCreative(String adAccountId, String accessToken, String caption, String imageUrl) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/adcreatives";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Creative_2");
        body.add("object_story_spec",
                String.format(
                        "{\"page_id\":\"666307613232481\",\"instagram_actor_id\":\"17841475488048649\",\"link_data\":{\"message\":\"%s\",\"link\":\"%s\"}}",
                        caption, imageUrl));
        body.add("access_token", accessToken);

        return postAndExtractId(url, body);
    }

    private void createAd(String adAccountId, String adSetId, String creativeId, String accessToken) {
        String url = "https://graph.facebook.com/v22.0/" + adAccountId + "/ads";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("name", "New Ad_2");
        body.add("adset_id", adSetId);
        body.add("creative", String.format("{\"creative_id\":\"%s\"}", creativeId));
        body.add("status", "PAUSED");
        body.add("access_token", accessToken);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, getHeaders()),
                String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Ad 생성 실패: " + response.getBody());
        }
    }

    private String postAndExtractId(String url, MultiValueMap<String, String> body) {
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, getHeaders()),
                String.class);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("ID 파싱 실패: " + response.getBody());
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private String getTargetingJson(String placementOption) {
        Map<String, Object> targeting = new HashMap<>();
        targeting.put("geo_locations", Collections.singletonMap("countries", Arrays.asList("KR")));

        switch (placementOption.toUpperCase()) {
            case "INSTAGRAM":
                targeting.put("publisher_platforms", Arrays.asList("instagram"));
                targeting.put("instagram_positions", Arrays.asList("story", "reels"));
                break;
            case "FACEBOOK":
                targeting.put("publisher_platforms", Arrays.asList("facebook"));
                targeting.put("facebook_positions", Arrays.asList("feed", "right_hand_column"));
                break;
            case "BOTH":
            default:
                targeting.put("publisher_platforms", Arrays.asList("facebook", "instagram"));
                targeting.put("facebook_positions", Arrays.asList("feed"));
                targeting.put("instagram_positions", Arrays.asList("feed", "story"));
                break;
        }

        try {
            return new ObjectMapper().writeValueAsString(targeting);
        } catch (Exception e) {
            throw new RuntimeException("타겟팅 JSON 생성 실패", e);
        }
    }
}
