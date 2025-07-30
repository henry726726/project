package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority; // UserInfoResponse에서 사용
import java.util.Collection; // UserInfoResponse에서 사용
import java.util.List; // 혹시 몰라서 추가 (TextGenerationResponse는 여기서 쓰이진 않지만, 통합 DTO를 고려할 때 다른 그룹에서 필요할 수 있음)

// 💡 사용자 인증 및 계정 관리에 관련된 모든 DTO를 한 파일에 모았습니다.
// 각 DTO는 UserAuthDtos 클래스 내부에 static public 클래스로 정의되어 독립적으로 사용될 수 있습니다.
public class UserAuthDtos {

    // LoginRequest.java의 내용
    @Getter
    @Setter
    public static class LoginRequest { // static public class로 선언
        private String email;
        private String password;
    }

    // LoginResponse.java의 내용
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor // 모든 필드(token, message)를 받는 생성자
    public static class LoginResponse { // static public class로 선언
        private String token;
        private String message;

        // 로그인 성공 시 토큰만 반환하는 편의 생성자
        public LoginResponse(String token) {
            this.token = token;
            this.message = null; // 성공 시 메시지는 null
        }
    }

    // SignupRequest.java의 내용
    @Getter
    @Setter
    public static class SignupRequest { // static public class로 선언
        private String email;
        private String password;
        private String nickname;
    }

    // UserInfoResponse.java의 내용
    @Getter
    @AllArgsConstructor
    public static class UserInfoResponse { // static public class로 선언
        private String email;
        private Collection<? extends GrantedAuthority> authorities;
    }
}