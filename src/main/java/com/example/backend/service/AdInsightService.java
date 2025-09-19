package com.example.backend.service;

import com.example.backend.entity.AdInsight;
import com.example.backend.repository.AdInsightRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AdInsightService {

    @Autowired
    private AdInsightRepository insightRepository;

    public void fetchAndStoreInsights(String adId, String accessToken) {
        // age, gender는 breakdowns로 요청해야 한다는 점 주의
        String url = String.format(
                "https://graph.facebook.com/v20.0/%s/insights?fields=impressions,clicks,spend,reach,cpc,ctr,frequency&date_preset=last_90d&breakdowns=age,gender&access_token=%s",
                adId, accessToken);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode dataArray = root.get("data");

            if (dataArray == null || !dataArray.isArray() || dataArray.size() == 0) {
                System.out.println("⚠️ 광고 성과 데이터가 없습니다.");
                return;
            }

            for (JsonNode node : dataArray) {
                AdInsight insight = new AdInsight();
                insight.setAdId(adId);

                String age = node.path("age").isMissingNode() ? null : node.path("age").asText();
                String gender = node.path("gender").isMissingNode() ? null : node.path("gender").asText();

                int impressions = node.path("impressions").asInt(0);
                int clicks = node.path("clicks").asInt(0);
                BigDecimal spend = new BigDecimal(node.path("spend").asText("0"));
                int reach = node.path("reach").asInt(0);
                BigDecimal cpc = new BigDecimal(node.path("cpc").asText("0"));
                BigDecimal ctr = new BigDecimal(node.path("ctr").asText("0"));
                BigDecimal frequency = new BigDecimal(node.path("frequency").asText("0"));

                insight.setAge(age);
                insight.setGender(gender);
                insight.setImpressions(impressions);
                insight.setClicks(clicks);
                insight.setSpend(spend);
                insight.setReach(reach);
                insight.setCpc(cpc);
                insight.setCtr(ctr);
                insight.setFrequency(frequency);
                insight.setDate(LocalDate.now());

                // 중복 데이터 저장 방지: 동일한 광고ID와 날짜 데이터가 있으면 저장하지 않음
                if (!insightRepository.existsByAdIdAndDate(insight.getAdId(), insight.getDate())) {
                    insightRepository.save(insight);
                } else {
                    System.out.println("⚠️ 중복 데이터 발견, 저장하지 않음: " + insight);
                }
            }

            System.out.println("✅ 성과 데이터 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 성과 데이터 저장 실패");
            e.printStackTrace();
        }
    }
}