package com.example.backend.controller;

import com.example.backend.dto.ContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserDataInput;
import com.example.backend.repository.UserDataInputRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userdatainput")
@CrossOrigin(origins = "*")
public class UserDataInputController {

    private final UserDataInputRepository repository;

    @Autowired
    public UserDataInputController(UserDataInputRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/content")
    public String saveContent(@RequestBody ContentRequest request) {
        System.out.println("🚀 [DEBUG] 수신된 요청: " + request);

        try {
            // 🔹 사용자 데이터 생성
            UserDataInput input = new UserDataInput();
            input.setId(request.getUserId()); // ✅ String 타입으로 바로 설정
            input.setName(request.getName());
            input.setProduct(request.getProduct());
            input.setTarget(request.getTarget());
            input.setPurpose(request.getPurpose());
            input.setKeyword(request.getKeyword());
            input.setDuration(request.getDuration());

            // 🔹 콘텐츠 생성 및 연관관계 설정
            Content content = new Content();
            content.setCaption(request.getCaption());
            content.setImageUrl(request.getImageUrl());

            input.addContent(content); // ✅ JPA 양방향 연관관계 안전 설정

            // 🔹 저장
            repository.save(input);

            return "✅ 사용자 입력 + 콘텐츠 저장 완료";

        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 에러 출력
            return "❌ 오류 발생: " + e.getMessage();
        }
    }
}
