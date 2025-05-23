package com.example.metaads.service;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.Campaign;
import org.springframework.stereotype.Service;

@Service
public class MetaAdsService {

    public APINodeList<AdAccount> getAdAccounts(String accessToken, String appSecret) throws Exception {
        APIContext context = new APIContext(accessToken, appSecret);
        // "me"는 현재 인증된 사용자에게 연결된 광고 계정을 의미합니다.
        return new AdAccount("me", context).getAdAccounts().execute();
    }

    public Campaign createCampaign(String accessToken, String appSecret, String adAccountId, String campaignName) throws Exception {
        APIContext context = new APIContext(accessToken, appSecret);
        AdAccount adAccount = new AdAccount(adAccountId, context);

        return adAccount.createCampaign()
                .setName(campaignName)
                .setObjective(Campaign.EnumObjective.VALUE_LINK_CLICKS) // 캠페인 목표 (예: 링크 클릭)
                .setStatus(Campaign.EnumStatus.VALUE_PAUSED) // 캠페인 상태 (일시 중지)
                .execute();
    }
}