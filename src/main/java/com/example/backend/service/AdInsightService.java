// AdInsightService.java (다시 한번 정확한 수정본)
package com.example.backend.service;

import com.example.backend.entity.AdInsight;
import com.example.backend.entity.Ad; // 💡 Ad 엔티티 임포트
import com.example.backend.repository.AdInsightRepository;
import com.example.backend.repository.AdRepository; // 💡 AdRepository 임포트
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

    @Autowired // 💡 AdRepository 주입
    private AdRepository adRepository;

    // 💡 매개변수명 변경: adId -> adIdFromParam (혼동 방지)
    public void fetchAndStoreInsights(String adIdFromParam, String accessToken) {
        String url = String.format(
                "https://graph.facebook.com/v20.0/%s/insights?fields=impressions,clicks,spend,reach,cpc,ctr,frequency&access_token=%s",
                adIdFromParam, accessToken);

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

            // 💡 adIdFromParam에 해당하는 Ad 엔티티를 미리 조회
            Ad ad = adRepository.findById(adIdFromParam) // AdRepository의 findById가 String ID를 받도록 되어있었음
                    .orElseThrow(() -> new RuntimeException("Ad not found with ID: " + adIdFromParam));


            for (JsonNode node : dataArray) {
                AdInsight insight = new AdInsight();
                insight.setAd(ad); // 💡 수정: setAdId(String) 대신 setAd(Ad) 사용

                // 안전하게 값 추출
                String age = node.path("age").isMissingNode() ? null : node.path("age").asText();
                String gender = node.path("gender").isMissingNode() ? null : node.path("gender").asText();

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