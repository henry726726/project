// src/main/java/com/example/backend/service/MetaAdCreatorService.java
package com.example.backend.service;

import com.example.backend.dto.AdAccountDto; // 💡 AdAccountDto 임포트
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional; // Optional 임포트가 없었다면 추가

@Service
@RequiredArgsConstructor
public class MetaAdCreatorService { // 💡 클래스명: MetaAdCreatorService

    private final UserRepository userRepository;

    @Transactional
    public void saveAdAccount(String userEmail, AdAccountDto adAccountDto) { // 💡 AdAccountDto 사용
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + userEmail));

        // ... 실제 Meta 광고 계정 생성/저장 로직 ...
        System.out.println("Meta Ad Account created/updated for user: " + userEmail);
        System.out.println("Account ID: " + adAccountDto.getAccountId());
        System.out.println("Access Token: " + adAccountDto.getAccessToken());

        // (추가) User 엔티티에 AdAccount 정보를 직접 저장하는 필드를 추가하거나,
        // 별도의 AdAccount 엔티티를 만들어 관리해야 합니다.
        // 현재 이 서비스는 AccessTokenEntity만 다루고 있지 않습니다.
        // 실제 광고 계정 정보를 저장하는 로직이 필요합니다.
    }
}