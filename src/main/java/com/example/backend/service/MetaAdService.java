package com.example.backend.service;

import com.example.backend.entity.AdAccount;
import com.example.backend.repository.AdAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MetaAdService {

    @Autowired
    private AdAccountRepository adAccountRepo;

    public void saveAdAccounts(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 1. 연결된 비즈니스 목록 조회
            String businessUrl = "https://graph.facebook.com/v20.0/me/businesses?access_token=" + accessToken;
            JsonNode businessJson = restTemplate.getForObject(businessUrl, JsonNode.class);
            JsonNode businessList = businessJson.path("data");

            if (businessList.isEmpty()) {
                System.out.println("❌ 연결된 비즈니스 계정이 없습니다.");
                return;
            }

            // 2. 페이지 목록 미리 조회
            String pageUrl = "https://graph.facebook.com/v20.0/me/accounts?access_token=" + accessToken;
            JsonNode pagesJson = restTemplate.getForObject(pageUrl, JsonNode.class);
            System.out.println("▶ 페이지 목록 응답:\n" + pagesJson.toPrettyString());

            // 3. 각 비즈니스에 대해 광고 계정 조회 및 저장
            for (JsonNode business : businessList) {
                String businessId = business.path("id").asText();

                String adAccountUrl = "https://graph.facebook.com/v20.0/" + businessId +
                        "/owned_ad_accounts?fields=id,account_id,name,status&access_token=" + accessToken;
                JsonNode adAccountsJson = restTemplate.getForObject(adAccountUrl, JsonNode.class);
                System.out.println("▶ 비즈니스 ID " + businessId + "의 광고 계정 응답:\n" + adAccountsJson.toPrettyString());

                for (JsonNode account : adAccountsJson.path("data")) {
                    String adId = account.path("id").asText();
                    String accountId = account.path("account_id").asText();
                    String name = account.path("name").asText();
                    String status = account.has("status") ? account.get("status").asText() : null;

                    for (JsonNode page : pagesJson.path("data")) {
                        String pageId = page.path("id").asText();

                        // Instagram ID 요청
                        String instaUrl = "https://graph.facebook.com/v20.0/" + pageId +
                                "?fields=instagram_business_account&access_token=" + accessToken;
                        JsonNode instaJson = restTemplate.getForObject(instaUrl, JsonNode.class);

                        String instagramId = null;
                        JsonNode instaNode = instaJson.get("instagram_business_account");
                        if (instaNode != null && instaNode.has("id")) {
                            instagramId = instaNode.get("id").asText();
                        }

                        // DB 저장
                        AdAccount ad = new AdAccount();
                        ad.setId(adId + "_" + pageId); // 중복 방지용 조합
                        ad.setAccountId(accountId);
                        ad.setName(name);
                        ad.setStatus(status);
                        ad.setPageId(pageId);
                        ad.setInstagramId(instagramId);

                        adAccountRepo.save(ad);
                    }
                }
            }

            System.out.println("✅ 비즈니스 포트폴리오 광고 계정 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 저장 중 오류 발생");
            e.printStackTrace();
        }
    }
}
