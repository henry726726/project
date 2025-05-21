package com.example.backend.service;

import com.example.backend.entity.Content;
import com.example.backend.entity.UserDataInput;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserDataInputRepository;

import jakarta.annotation.PostConstruct;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserDataInputService {

    @Autowired
    private UserDataInputRepository userdataRepo;

    @Autowired
    private ContentRepository contentRepo;

    public void saveContent(String userdatainputId, String caption, String imageUrl) {
        System.out.println("🟡 저장 시도: " + caption + " / " + imageUrl);
        // 문구 + url을 통해서 고유한 해시값(id) 만들어냄
        // 문구랑 url이 같으면 동일한 해시값 생성되므로 중복 방지
        String hash = DigestUtils.sha256Hex(caption + imageUrl);
        if (contentRepo.existsById(hash))
            return;

        UserDataInput input = userdataRepo.findById(userdatainputId)
                .orElseThrow(() -> new RuntimeException("UserDataInput not found"));

        Content content = new Content();
        content.setId(hash);
        content.setCaption(caption);
        content.setImageUrl(imageUrl);
        content.setCreatedAt(LocalDateTime.now());
        content.setUserdatainput(input);

        contentRepo.save(content);

        System.out.println("✅ 저장 요청: " + caption);
        System.out.println("✅ 저장 ID: " + hash);
        System.out.println("✅ 저장 대상 userdatainputId: " + userdatainputId);

    }

    // 테스트용 코드(추후삭제)
    @PostConstruct
    public void init() {
        // 이미 존재하는 캠페인 불러오기
        UserDataInput input = userdataRepo.findById("test-001")
                .orElseThrow(() -> new RuntimeException("❌ 캠페인 test-001이 존재하지 않습니다."));

        // 테스트용 콘텐츠 여러 개 준비
        List<String[]> testContents = new ArrayList<>();
        testContents.add(new String[] { "AI가 만든 문구 1", "https://cdn.site.com/image1.jpg" });
        testContents.add(new String[] { "AI가 만든 문구 2", "https://cdn.site.com/image2.jpg" });
        testContents.add(new String[] { "AI가 만든 문구 1", "https://cdn.site.com/image1.jpg" }); // 중복

        for (String[] pair : testContents) {
            String caption = pair[0];
            String imageUrl = pair[1];
            String hash = DigestUtils.sha256Hex(caption + imageUrl);

            // 중복 콘텐츠는 건너뜀
            if (!contentRepo.existsById(hash)) {
                Content c = new Content();
                c.setId(hash);
                c.setCaption(caption);
                c.setImageUrl(imageUrl);
                c.setCreatedAt(LocalDateTime.now());
                c.setUserdatainput(input);
                contentRepo.save(c);
                System.out.println("✅ 콘텐츠 저장됨: " + caption);
            } else {
                System.out.println("⚠️ 중복 콘텐츠 건너뜀: " + caption);
            }
        }
    }
}
