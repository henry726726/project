package com.example.backend.service;

import com.example.backend.dto.ContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserDataInput;
import com.example.backend.repository.UserDataInputRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserDataInputService {

    private final UserDataInputRepository repository;

    @Autowired
    public UserDataInputService(UserDataInputRepository repository) {
        this.repository = repository;
    }

    public void saveUserDataWithContent(ContentRequest request) {
        // 🔹 사용자 입력 정보 저장용 객체 생성
        UserDataInput input = new UserDataInput();
        input.setId(request.getUserId());
        input.setName(request.getName());
        input.setProduct(request.getProduct());
        input.setTarget(request.getTarget());
        input.setPurpose(request.getPurpose());
        input.setKeyword(request.getKeyword());
        input.setDuration(request.getDuration());

        // 🔹 문구 + 이미지 정보 객체 생성
        Content content = new Content();
        content.setCaption(request.getCaption());
        content.setImageUrl(request.getImageUrl());
        content.setUserdatainput(input);       // 양방향 연결
        input.getContents().add(content);

        // 🔹 저장 (연관된 Content도 함께 저장됨)
        repository.save(input);
    }
}
