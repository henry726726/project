package com.example.backend.controller;

import com.example.backend.dto.AdContentStorageDtos.ContentRequest; // UserDataInputController에서 사용
import com.example.backend.dto.AdContentStorageDtos.SaveAdContentRequest; // AdContentController에서 사용
import com.example.backend.entity.AdContent; // AdContentController에서 사용
import com.example.backend.entity.Content; // UserDataInputController에서 사용
import com.example.backend.entity.UserDataInput; // UserDataInputController에서 사용
import com.example.backend.repository.UserDataInputRepository; // UserDataInputController에서 사용
import com.example.backend.service.AdContentService; // AdContentController에서 사용
import lombok.RequiredArgsConstructor; // AdContentController에서 사용
import org.springframework.beans.factory.annotation.Autowired; // UserDataInputController에서 사용 (RequiredArgsConstructor 대체 가능)
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor // AdContentService, UserDataInputRepository 주입 (AuthAndUserController처럼)
public class ContentStorageController {

    // --- AdContentController에서 온 필드 ---
    private final AdContentService adContentService;

    // --- UserDataInputController에서 온 필드 ---
    private final UserDataInputRepository userDataInputRepository; // 이름 충돌 방지: 기존 repository -> userDataInputRepository


    // =======================================================
    // 💡 AdContentController의 기능
    // 기본 경로: /api/ad-content
    // =======================================================
    @RestController // 내부 클래스도 RestController 역할을 해야 합니다.
    @RequestMapping("/api/ad-content")
    public class AdContentSubController {

        // POST /api/ad-content/save 요청을 처리할 메서드
        @PostMapping("/save")
        public ResponseEntity<String> saveAdContent(@RequestBody SaveAdContentRequest request) {
            try {
                AdContent savedContent = adContentService.saveAdContent(request);
                System.out.println("광고 콘텐츠 저장 성공! ID: " + savedContent.getId());
                return ResponseEntity.ok("광고 콘텐츠가 성공적으로 저장되었습니다. ID: " + savedContent.getId());
            } catch (Exception e) {
                System.err.println("광고 콘텐츠 저장 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("광고 콘텐츠 저장 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    // =======================================================
    // 💡 UserDataInputController의 기능
    // 기본 경로: /userdatainput
    // =======================================================
    @RestController // 내부 클래스도 RestController 역할을 해야 합니다.
    @RequestMapping("/userdatainput")
    @CrossOrigin(origins = "*") // UserDataInputController에 있던 CORS 설정
    public class UserDataInputSubController {

        @PostMapping("/content")
        public String saveContent(@RequestBody ContentRequest request) {
            System.out.println("🚀 [DEBUG] 수신된 요청: " + request);
            try {
                UserDataInput input = new UserDataInput();
                input.setId(request.getUserId());
                input.setName(request.getName());
                input.setProduct(request.getProduct());
                input.setTarget(request.getTarget());
                input.setPurpose(request.getPurpose());
                input.setKeyword(request.getKeyword());
                input.setDuration(request.getDuration());

                Content content = new Content();
                content.setCaption(request.getCaption());
                content.setImageUrl(request.getImageUrl());

                input.addContent(content);

                userDataInputRepository.save(input);

                return "✅ 사용자 입력 + 콘텐츠 저장 완료";
            } catch (Exception e) {
                e.printStackTrace();
                return "❌ 오류 발생: " + e.getMessage();
            }
        }
    }
}