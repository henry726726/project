package com.example.backend.controller;

import com.example.backend.dto.ChangePasswordRequest;
import com.example.backend.dto.ChangeNicknameRequest;
import com.example.backend.dto.UserInfoResponse;
import com.example.backend.service.AuthService; // 💡 수정: UserService 대신 AuthService 임포트
import com.example.backend.entity.User; // 💡 추가: User 엔티티 임포트 (닉네임 조회를 위해)
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService; // 💡 수정: userService 대신 authService 주입

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        // 💡 수정: AuthService를 통해 User 엔티티를 조회하여 닉네임을 가져옴
        User user = authService.getUserByEmail(userDetails.getUsername()); // userDetails.getUsername()은 이메일
        
        UserInfoResponse response = new UserInfoResponse(
                user.getEmail(),       // 이메일
                user.getNickname(),    // 💡 닉네임 (User 엔티티에 getNickname() 메소드 필요)
                userDetails.getAuthorities()); // 권한

        return ResponseEntity.ok(response);
    }

    @PostMapping("/nickname-change")
    public ResponseEntity<String> changeNickname(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangeNicknameRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }
        try {
            // 💡 수정: authService를 통해 닉네임 변경 로직 호출
            authService.changeNickname(userDetails.getUsername(), request.getNewNickname());
            return ResponseEntity.ok("닉네임이 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("닉네임 변경 중 예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("닉네임 변경 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/password-change")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }
        try {
            // 💡 수정: authService를 통해 비밀번호 변경 로직 호출
            authService.changePassword(
                    userDetails.getUsername(),
                    request.getCurrentPassword(),
                    request.getNewPassword());
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("비밀번호 변경 중 예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }
}