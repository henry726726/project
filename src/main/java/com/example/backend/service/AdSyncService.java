package com.example.backend.service;

import com.example.backend.entity.Ad;
import com.example.backend.entity.AdAccount;
import com.example.backend.repository.AdRepository;
import com.example.backend.repository.AdAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AdSyncService {

    @Autowired
    private AdRepository adRepository;

    @Autowired
    private AdAccountRepository adAccountRepository;

    public void syncAdsFromMeta(String adAccountId, String accessToken) {
        String apiUrl = String.format(
                "https://graph.facebook.com/v20.0/act_%s/ads?access_token=%s&fields=id,name,status",
                adAccountId, accessToken);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            System.out.println("▶ API 호출 주소: " + apiUrl);

            String json = restTemplate.getForObject(apiUrl, String.class);
            System.out.println("▶ API 응답 결과: " + json);

            JsonNode root = objectMapper.readTree(json);
            JsonNode dataArray = root.get("data");

            System.out.println("▶ 응답된 광고 수: " + dataArray.size());

            // DB에서 adAccount 불러오기
            AdAccount adAccount = adAccountRepository.findByAccountId(adAccountId)
                    .orElseThrow(() -> new RuntimeException("❌ AdAccount not found: " + adAccountId));

            System.out.println("▶ DB에서 찾은 AdAccount: " + adAccount.getAccountId());

            for (JsonNode adNode : dataArray) {
                String adId = adNode.get("id").asText();
                String name = adNode.get("name").asText();
                String status = adNode.get("status").asText();

                if (adRepository.existsByAdId(adId)) {
                    System.out.println("⚠️ 이미 존재하는 광고: " + adId);
                    continue;
                }

                Ad ad = new Ad();
                ad.setAdId(adId);
                ad.setName(name);
                ad.setStatus(status);
                ad.setAdAccount(adAccount);

                System.out.println(
                        "👉 저장할 광고: " + adId + ", " + name + ", " + status + ", account: " + adAccount.getAccountId());

                adRepository.save(ad);
            }

            System.out.println("✅ 광고 목록 DB 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 광고 목록 동기화 실패");
            e.printStackTrace();
        }
    }
}
