package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter; // 💡 Lombok Getter 추가
import lombok.Setter; // 💡 Lombok Setter 추가
import lombok.NoArgsConstructor; // 💡 Lombok NoArgsConstructor 추가

@Entity
@Getter // Lombok이 Getters 생성
@Setter // Lombok이 Setters 생성
@NoArgsConstructor // Lombok이 기본 생성자 생성
public class UserDataInput {

    @Id
    private String id; // ex: 프론트에서 넘긴 유저 ID (userId, UUID 등)

    private String name; // 사용자 이름 (또는 닉네임)
    private String product; // 제품명
    private String target; // 타겟 (ex. 30대 여성)
    private String purpose; // 목적 (구매 유도 등)
    private String keyword; // 강조 키워드
    private String duration; // 광고 기간

    private LocalDateTime createdAt = LocalDateTime.now();

    // 🔁 연관 콘텐츠 리스트 (1:N)
    @OneToMany(mappedBy = "userdatainput", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Content> contents = new ArrayList<>();

    // 💡 Lombok이 NoArgsConstructor와 Getters/Setters를 제공하므로 아래는 불필요

    // 생성자 (모든 필드 포함 생성자는 수동으로 유지하거나 Lombok @AllArgsConstructor 사용)
    public UserDataInput(String id, String name, String product, String target,
            String purpose, String keyword, String duration) {
        this.id = id;
        this.name = name;
        this.product = product;
        this.target = target;
        this.purpose = purpose;
        this.keyword = keyword;
        this.duration = duration;
        // createdAt은 필드 초기화에서 LocalDateTime.now()로 이미 설정됨
    }

    // 편의 메서드들은 그대로 유지하는 것이 좋습니다.
    public void addContent(Content content) {
        contents.add(content);
        content.setUserdatainput(this);
    }

    public void removeContent(Content content) {
        contents.remove(content);
        content.setUserdatainput(null);
    }
}