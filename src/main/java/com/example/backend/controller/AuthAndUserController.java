package com.example.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping; // @RequestMapping 임포트 필요
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.UserAuthDtos.LoginRequest;
import com.example.backend.dto.UserAuthDtos.LoginResponse;
import com.example.backend.dto.UserAuthDtos.SignupRequest;
import com.example.backend.dto.UserAuthDtos.UserInfoResponse;
import com.example.backend.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController // 💡 메인 클래스에 RestController
@RequestMapping // 💡 여기에 @RequestMapping("/auth") 또는 @RequestMapping("/user")를 사용할 수 있습니다.
                //    일단은 각 메소드에 직접 붙이거나, 필요한 경우 클래스 레벨에서 /auth 만 정의하고 나머지는 별도 컨트롤러로 분리할 수도 있습니다.
                //    예를 들어, @RequestMapping("/auth")를 여기에 붙이면 모든 @PostMapping/@GetMapping은 /auth/... 가 됩니다.
                //    이 예시에서는 메소드에 직접 붙여서 명확하게 합니다.
@RequiredArgsConstructor
public class AuthAndUserController {

    private final AuthService authService;

    // ============== AuthController의 기능 (AuthAndUserController로 직접 통합) ==============

    // 회원가입 API
    @PostMapping("/auth/signup") // 💡 /auth/signup 경로 직접 지정
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            authService.signup(request);
            return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // 로그인 API
    @PostMapping("/auth/login") // 💡 /auth/login 경로 직접 지정
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request);
            // 💡 로그인 성공 시, JWT 토큰을 포함한 응답을 LoginResponse 객체로 반환 (응답 상태 200 OK)
            return ResponseEntity.ok(new LoginResponse(token, "로그인 성공"));
        } catch (IllegalArgumentException e) {
            // 💡 로그인 실패 시, 에러 메시지를 포함한 응답을 LoginResponse 객체로 반환 (응답 상태 401 UNAUTHORIZED)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse(null, e.getMessage()));
        }
    }

    // ============== UserController의 기능 (AuthAndUserController로 직접 통합) ==============

    @GetMapping("/user/me") // 💡 /user/me 경로 직접 지정
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        // 💡 사용자 상세 정보 조회 서비스 호출 (AuthService에 해당 메서드 추가 필요)
        // 예를 들어, 사용자 닉네임을 포함한 정보
        // User currentUser = authService.getUserByEmail(userDetails.getUsername());
        UserInfoResponse response = new UserInfoResponse(
                userDetails.getUsername(),
                userDetails.getAuthorities());
                // currentUser.getNickname() // 💡 닉네임 필드가 있다면 이렇게 추가

        return ResponseEntity.ok(response);
    }

    // 💡 기타 User 관련 API (예시)
    // @PostMapping("/user/nickname-change")
    // public ResponseEntity<?> changeNickname(...) { ... }
}