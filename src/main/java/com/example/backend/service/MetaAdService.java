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
            // 1. 광고 계정 조회
            String adAccountUrl = "https://graph.facebook.com/v20.0/me/adaccounts?fields=id,account_id,name,status&access_token="
                    + accessToken;
            JsonNode adAccountsJson = restTemplate.getForObject(adAccountUrl, JsonNode.class);
            System.out.println("▶ 광고 계정 응답: \n" + adAccountsJson.toPrettyString());

            // 2. 페이지 조회
            String pageUrl = "https://graph.facebook.com/v20.0/me/accounts?access_token=" + accessToken;
            JsonNode pagesJson = restTemplate.getForObject(pageUrl, JsonNode.class);
            System.out.println("▶ 페이지 목록 응답: \n" + pagesJson.toPrettyString());

            // 3. 광고 계정 * 페이지 조합으로 저장
            for (JsonNode account : adAccountsJson.path("data")) {
                String adId = account.path("id").asText();
                String accountId = account.path("account_id").asText();
                String name = account.path("name").asText();
                String status = account.has("status") ? account.get("status").asText() : null;

                for (JsonNode page : pagesJson.path("data")) {
                    String pageId = page.path("id").asText();

                    // Instagram ID 요청
                    String instaUrl = "https://graph.facebook.com/v20.0/" + pageId
                            + "?fields=instagram_business_account&access_token=" + accessToken;
                    JsonNode instaJson = restTemplate.getForObject(instaUrl, JsonNode.class);

                    String instagramId = null;
                    JsonNode instaNode = instaJson.get("instagram_business_account");
                    if (instaNode != null && instaNode.has("id")) {
                        instagramId = instaNode.get("id").asText();
                    }

                    // DB 저장
                    AdAccount ad = new AdAccount();
                    ad.setId(adId + "_" + pageId); // 고유키 중복 방지를 위해 ID 조합
                    ad.setAccountId(accountId);
                    ad.setName(name);
                    ad.setStatus(status);
                    ad.setPageId(pageId);
                    ad.setInstagramId(instagramId);

                    adAccountRepo.save(ad);
                }
            }

            System.out.println("✅ 광고 계정 × 페이지 × 인스타 ID 저장 완료");

        } catch (Exception e) {
            System.out.println("❌ 저장 중 오류 발생");
            e.printStackTrace();
        }
    }

}
