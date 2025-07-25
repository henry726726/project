package com.example.backend.service;

import com.example.backend.dto.SaveAdContentRequest;
import com.example.backend.entity.AdContent;
import com.example.backend.repository.AdContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 광고 콘텐츠 저장 비즈니스 로직을 담당하는 서비스
@Service
@RequiredArgsConstructor
public class AdContentService {

    private final AdContentRepository adContentRepository;

    @Transactional
    public AdContent saveAdContent(SaveAdContentRequest request) {
        // SaveAdContentRequest DTO를 AdContent 엔티티로 변환
        AdContent adContent = new AdContent();
        adContent.setProduct(request.getProduct());
        adContent.setTarget(request.getTarget());
        adContent.setPurpose(request.getPurpose());
        adContent.setKeyword(request.getKeyword());
        adContent.setDuration(request.getDuration());
        // ✅ 여기를 수정하세요: adContent.adText() -> adContent.setAdText()
        adContent.setAdText(request.getAdText());
        adContent.setGeneratedImageBase64(request.getGeneratedImageBase64());

        // 리포지토리를 통해 데이터베이스에 저장
        AdContent savedAdContent = adContentRepository.save(adContent);

        // 저장된 엔티티 반환
        return savedAdContent;
    }
}