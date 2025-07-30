package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter; // 💡 Lombok Getter 추가
import lombok.Setter; // 💡 Lombok Setter 추가
import lombok.NoArgsConstructor; // 💡 Lombok NoArgsConstructor 추가

@Entity
@Getter // Lombok이 Getters 생성
@Setter // Lombok이 Setters 생성
@NoArgsConstructor // Lombok이 기본 생성자 생성
public class Content {

    @Id
    private String id;

    @ManyToOne // Content와 UserDataInput의 N:1 관계
    @JoinColumn(name = "userdatainput_id") // 외래키 이름 지정
    private UserDataInput userdatainput;

    private String caption;
    @Column(length = 1000)
    private String imageUrl;
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시간 기본값

    // 💡 Lombok이 NoArgsConstructor와 Getters/Setters를 제공하므로 아래는 불필요
}