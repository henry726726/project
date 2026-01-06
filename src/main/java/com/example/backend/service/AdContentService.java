package com.example.backend.service;

import com.example.backend.dto.SaveAdContentRequest;
import com.example.backend.entity.AdContent;
import com.example.backend.entity.User;
import com.example.backend.repository.AdContentRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdContentService {

    private final AdContentRepository adContentRepository;
    private final UserRepository userRepository;

    // 생성자 주입
    public AdContentService(
            AdContentRepository adContentRepository,
            UserRepository userRepository) {
        this.adContentRepository = adContentRepository;
        this.userRepository = userRepository;
    }

    public AdContent findByIdOrThrow(Long id) {
        return adContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found: " + id));
    }

    public AdContent saveAdContent(SaveAdContentRequest request, String userEmail) {

        // 이메일 → User 엔티티 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        // AdContent 생성
        AdContent adContent = new AdContent();
        adContent.setUser(user); // FK
        adContent.setUserEmail(userEmail); // 보조 정보

        // 광고 정보
        adContent.setAdText(request.getAdText());
        adContent.setGeneratedImageBase64(request.getGeneratedImageBase64());
        adContent.setOriginalImageBase64(request.getOriginalImageBase64());
        adContent.setProduct(request.getProduct());
        adContent.setTarget(request.getTarget());
        adContent.setPurpose(request.getPurpose());
        adContent.setKeyword(request.getKeyword());
        adContent.setDuration(request.getDuration());

        return adContentRepository.save(adContent);
    }
}
