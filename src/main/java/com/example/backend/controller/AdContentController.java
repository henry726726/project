package com.example.backend.controller;

import com.example.backend.dto.SaveAdContentRequest; // SaveAdContentRequest DTO를 임포트합니다.
import com.example.backend.entity.AdContent; // AdContent 엔티티를 임포트합니다.
import com.example.backend.service.AdContentService; // AdContentService를 임포트합니다.
import org.springframework.http.HttpStatus; // HTTP 상태 코드 정의를 위해 임포트합니다.
import org.springframework.http.ResponseEntity; // HTTP 응답을 구성하기 위해 임포트합니다.
import org.springframework.security.core.Authentication; // 💡 현재 로그인된 사용자 정보를 가져오기 위한 Authentication 임포트
import org.springframework.security.core.context.SecurityContextHolder; // 💡 SecurityContextHolder를 통해 인증 정보에 접근하기 위해 임포트
import org.springframework.web.bind.annotation.PostMapping; // HTTP POST 요청 매핑을 위해 임포트합니다.
import org.springframework.web.bind.annotation.RequestBody; // HTTP 요청 본문을 객체로 변환하기 위해 임포트합니다.
import org.springframework.web.bind.annotation.RequestMapping; // 컨트롤러의 기본 URL 경로 지정을 위해 임포트합니다.
import org.springframework.web.bind.annotation.RestController; // RESTful 웹 서비스 컨트롤러임을 선언하기 위해 임포트합니다.

// 광고 콘텐츠 저장 API를 처리하는 컨트롤러
@RestController // 이 클래스가 RESTful 웹 서비스의 컨트롤러임을 나타냅니다.
@RequestMapping("/api/ad-content") // 이 컨트롤러의 모든 핸들러 메소드는 "/api/ad-content" 경로 아래에 매핑됩니다.
public class AdContentController {

    private final AdContentService adContentService; // AdContentService 인스턴스를 주입받습니다. (final 키워드로 필수 의존성임을 명시)

    // 💡💡💡 수동 생성자: AdContentService 의존성을 주입받아 초기화합니다. 💡💡💡
    // Spring이 이 생성자를 통해 adContentService 빈을 자동으로 주입합니다.
    public AdContentController(AdContentService adContentService) {
        this.adContentService = adContentService;
    }

    // POST /api/ad-content/save 요청을 처리할 메소드
    @PostMapping("/save") // 이 메소드는 HTTP POST 요청 중 "/api/ad-content/save" 경로에 매핑됩니다.
    public ResponseEntity<String> saveAdContent(@RequestBody SaveAdContentRequest request) { // HTTP 요청 본문을 SaveAdContentRequest 객체로 변환합니다.
        try {
            // 💡💡💡 현재 로그인된 사용자 정보 (이메일) 가져오기 💡💡💡
            // SecurityContextHolder에서 현재 인증된 사용자(Principal) 객체를 가져옵니다.
            // 이 Principal은 JWT 토큰 검증 시 Spring Security 컨텍스트에 설정된 것입니다.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증 객체로부터 사용자의 주체(principal)인 이메일을 가져옵니다.
            // JWT의 'sub' (subject) 클레임에 해당하는 이메일 주소가 저장되어 있습니다.
            String userEmail = authentication.getName(); 

            System.out.println("광고 콘텐츠 저장 요청 - 사용자 이메일: " + userEmail); // 디버깅을 위해 사용자 이메일을 콘솔에 출력합니다.

            // Service 계층에 데이터 저장 요청을 위임합니다.
            // 이때 프론트엔드에서 받은 요청 데이터(request)와 현재 로그인된 사용자의 이메일(userEmail)을 함께 전달합니다.
            AdContent savedContent = adContentService.saveAdContent(request, userEmail); // 💡 userEmail 전달

            // 저장 성공 시 콘솔에 성공 메시지를 출력하고, 클라이언트에게 200 OK 응답과 성공 메시지를 반환합니다.
            System.out.println("광고 콘텐츠 저장 성공! ID: " + savedContent.getId());
            return ResponseEntity.ok("광고 콘텐츠가 성공적으로 저장되었습니다. ID: " + savedContent.getId());
        } catch (Exception e) { // 저장 과정에서 예외 발생 시
            // 에러 메시지를 콘솔에 출력하고, 스택 트레이스를 출력합니다.
            System.err.println("광고 콘텐츠 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            // 클라이언트에게 500 INTERNAL_SERVER_ERROR 응답과 함께 에러 메시지를 반환합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("광고 콘텐츠 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}