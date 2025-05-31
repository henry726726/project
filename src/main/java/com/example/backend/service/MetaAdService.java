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
        String url = "https://graph.facebook.com/v20.0/me/adaccounts?fields=id,account_id,name,status&access_token="
                + accessToken;

        RestTemplate restTemplate = new RestTemplate();
        JsonNode json = restTemplate.getForObject(url, JsonNode.class);
        System.out.println(json.toPrettyString());

        for (JsonNode account : json.path("data")) {
            AdAccount ad = new AdAccount();
            ad.setId(account.path("id").asText());
            ad.setAccountId(account.path("account_id").asText());
            ad.setName(account.path("name").asText());
            ad.setStatus(account.path("status").asText());

            adAccountRepo.save(ad);
        }
    }
}
