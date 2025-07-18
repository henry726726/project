package com.example.backend.controller;

import com.example.backend.dto.UserInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // 이 경우는 SecurityConfig에서 authenticated()로 막히기 때문에 실제로 발생하기 어려움
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        // CustomUserDetailsService에서 UserDetails의 username으로 email을 반환하도록 설정했으므로,
        // userDetails.getUsername()이 곧 email이 됨.
        UserInfoResponse response = new UserInfoResponse(
            userDetails.getUsername(),  // email
            userDetails.getAuthorities() // 권한 정보
        );

        return ResponseEntity.ok(response);
    }
}