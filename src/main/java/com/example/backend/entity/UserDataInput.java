package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.backend.entity.Content;

@Entity
public class UserDataInput {

    @Id
    private String id;

    private String name;

    private LocalDateTime createdAt = LocalDateTime.now();

    // mappedBy = "userdatainput": Content 쪽의 필드명과 연결
    // CascadeType.ALL: 아이디 삭제하면 관련 콘텐츠도 자동 삭제
    @OneToMany(mappedBy = "userdatainput", cascade = CascadeType.ALL)
    private List<Content> contents = new ArrayList<>();

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
}
