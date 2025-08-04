package com.example.backend.entity;

import jakarta.persistence.*; // JPA 어노테이션 임포트
import lombok.Getter;       // Lombok @Getter 임포트
import lombok.Setter;       // Lombok @Setter 임포트
import lombok.NoArgsConstructor; // 필요에 따라 추가
import lombok.AllArgsConstructor; // 필요에 따라 추가 (생성자 이미 있다면 제거)

@Entity
// @Getter // 💡 이 어노테이션은 이제 수동 Getter를 추가하므로 제거해도 됨.
// @Setter // 💡 이 어노테이션은 이제 수동 Setter를 추가하므로 제거해도 됨.
@Table(name = "users") // 테이블 이름 지정 (선택 사항)
@NoArgsConstructor // JPA를 위한 기본 생성자
@AllArgsConstructor // 모든 필드를 포함하는 생성자 (필요 시)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // 이메일은 유니크하고 필수
    private String email;

    @Column(nullable = false) // 비밀번호는 필수
    private String password;

    @Column(unique = true) // 닉네임은 유니크할 수 있음 (선택 사항)
    private String nickname;

    // 편의 생성자 (Signup에서 사용)
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    // 💡💡💡 Lombok이 생성해주던 Getter/Setter 메소드들을 직접 추가합니다. 💡💡💡
    // 모든 필드에 대해 Getter/Setter를 추가해야 합니다.
    // 현재 오류는 setPassword(String)와 관련된 것이지만, 전체를 추가하는 게 좋습니다.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) { // 💡 setPassword(String) 메소드 (오류의 주범!)
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) { // 💡 setNickname(String) 메소드
        this.nickname = nickname;
    }
}