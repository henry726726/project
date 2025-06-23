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
import java.util.List;

@Service
public class AdInsightService {

    @Autowired
    private AdInsightRepository insightRepository;

    public void fetchAndStoreInsights(String adId, String accessToken) {
        String url = String.format(
                "https://graph.facebook.com/v20.0/%s/insights?fields=impressions,clicks,spend,reach,cpc,ctr,frequency,age,gender&access_token=%s",
                adId, accessToken);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode dataArray = root.get("data");

            for (JsonNode node : dataArray) {
                AdInsight insight = new AdInsight();
                insight.setAdId(adId);
                String age = node.get("age").asText();
                String gender = node.get("gender").asText();

                int impressions = Integer.parseInt(node.get("impressions").asText("0"));
                int clicks = Integer.parseInt(node.get("clicks").asText("0"));
                BigDecimal spend = new BigDecimal(node.get("spend").asText("0"));
                int reach = Integer.parseInt(node.get("reach").asText("0"));
                BigDecimal cpc = new BigDecimal(node.get("cpc").asText("0"));
                BigDecimal ctr = new BigDecimal(node.get("ctr").asText("0"));
                BigDecimal frequency = new BigDecimal(node.get("frequency").asText("0"));

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

                System.out.println("🧾 저장 데이터: " + insight);
                insightRepository.save(insight);
            }

            System.out.println("✅ 성과 데이터 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 성과 데이터 저장 실패");
            e.printStackTrace();
        }
    }
}