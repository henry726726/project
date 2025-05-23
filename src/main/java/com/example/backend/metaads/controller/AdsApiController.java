package com.example.metaads.controller;

import com.example.metaads.service.MetaAdsService;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.Campaign;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api")
public class AdsApiController {

    private final MetaAdsService metaAdsService;

    // application.yml에서 Meta 앱 시크릿 가져오기
    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String appSecret;

    public AdsApiController(MetaAdsService metaAdsService) {
        this.metaAdsService = metaAdsService;
    }

    @GetMapping("/test")
    public String testMetaAdsApi(
            @RegisteredOAuth2AuthorizedClient("facebook") OAuth2AuthorizedClient authorizedClient,
            Model model) {
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            // 1. 광고 계정 조회
            APINodeList<AdAccount> adAccounts = metaAdsService.getAdAccounts(accessToken, appSecret);
            String adAccountId = null;
            if (adAccounts != null && !adAccounts.isEmpty()) {
                adAccountId = adAccounts.get(0).getId(); // 첫 번째 광고 계정 ID 사용
                model.addAttribute("adAccounts", adAccounts.stream()
                        .map(acc -> acc.getName() + " (ID: " + acc.getId() + ")")
                        .collect(Collectors.toList()));
                model.addAttribute("message", "광고 계정 조회 성공!");
            } else {
                model.addAttribute("message", "광고 계정을 찾을 수 없습니다. 테스트를 위해 먼저 광고 계정을 만드세요.");
            }

            // 2. 캠페인 생성 (광고 계정 ID가 있을 경우에만 시도)
            if (adAccountId != null) {
                String campaignName = "Test Campaign " + System.currentTimeMillis();
                Campaign newCampaign = metaAdsService.createCampaign(accessToken, appSecret, adAccountId, campaignName);
                model.addAttribute("campaignResult", "캠페인 생성 성공: " + newCampaign.getName() + " (ID: " + newCampaign.getId() + ")");
            }

        } catch (Exception e) {
            model.addAttribute("error", "API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return "result"; // 결과 페이지로 이동
    }
}