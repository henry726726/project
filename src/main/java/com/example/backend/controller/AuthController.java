/*package com.example.backend.controller;


// import com.example.backend.service.AuthService; // ✅ 이 줄을 통째로 주석 처리합니다.

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import lombok.RequiredArgsConstructor; // ✅ 이 어노테이션 임포트도 통째로 주석 처리합니다.


@RestController
@RequestMapping("/auth")

// @RequiredArgsConstructor // ✅ 이 어노테이션 자체도 통째로 주석 처리합니다.

public class AuthController {

    
    // private final AuthService authService; // ✅ 이 필드 선언도 통째로 주석 처리합니다.
    

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody String token) {
        
        // authService.logout(token); // ✅ 이 호출도 통째로 주석 처리합니다.
        
        System.out.println("로그아웃 요청 수신됨. 토큰: " + token + " (임시 처리)");
        return ResponseEntity.ok("로그아웃 기능 임시 비활성화됨");
    }

    // 만약 로그인이나 회원가입 같은 다른 메서드도 있었다면,
    // 해당 메서드들의 내용도 모두 주석 처리하거나, 아예 메서드 전체를 주석 처리합니다.
    
    // @PostMapping("/login")
    // public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    //     // return authService.login(request);
    //     return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    // }
    
}
*/