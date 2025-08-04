// src/main/java/com/example/backend/dto/SignupRequest.java (전체 코드)
package com.example.backend.dto;

// import lombok.Getter; // 💡 필요하면 제거
// import lombok.Setter; // 💡 필요하면 제거
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// @Getter // 💡 이 어노테이션은 이제 수동 Getter를 추가하므로 제거해도 됨.
// @Setter // 💡 이 어노테이션은 이제 수동 Setter를 추가하므로 제거해도 됨.
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String email;
    private String password;
    private String nickname;

    // 💡💡💡 Lombok이 생성해주던 Getter/Setter 메소드들을 직접 추가합니다. 💡💡💡

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}