package com.example.backend.metaads.service;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.Campaign;
import com.facebook.ads.sdk.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MetaAdsService {

    // 필드 목록은 문자열 리스트로 유지
    private static final List<String> AD_ACCOUNT_FIELDS = Arrays.asList(
        "id",
        "name"
    );

    private static final List<String> CAMPAIGN_FIELDS = Arrays.asList(
        "id",
        "name"
    );


    public APINodeList<AdAccount> getAdAccounts(String accessToken, String appSecret) throws Exception {
        APIContext context = new APIContext(accessToken, appSecret);

        // requestFields()에 List<String> 타입의 AD_ACCOUNT_FIELDS를 전달합니다.
        return new User("me", context).getAdAccounts()
                                      .requestFields(AD_ACCOUNT_FIELDS)
                                      .execute();
    }

    public Campaign createCampaign(String accessToken, String appSecret, String adAccountId, String campaignName) throws Exception {
        APIContext context = new APIContext(accessToken, appSecret);
        AdAccount adAccount = new AdAccount(adAccountId, context);

        return adAccount.createCampaign()
                .setName(campaignName)
                .setObjective(Campaign.EnumObjective.VALUE_LINK_CLICKS)
                .setStatus(Campaign.EnumStatus.VALUE_PAUSED)
                .requestFields(CAMPAIGN_FIELDS)
                .execute();
    }
}