package com.example.backend.controller;

import com.example.backend.service.MetaAdCreatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta")
public class MetaAdCreateController {

    @Autowired
    private MetaAdCreatorService metaAdCreatorService;

    @PostMapping("/create")
    public String createAd(@RequestParam String contentId,
            @RequestParam(defaultValue = "BOTH") String placementOption) {
        metaAdCreatorService.createInitialAdByContentId(contentId, placementOption);
        return "광고 생성 요청 완료 (" + placementOption + ")";
    }
}
