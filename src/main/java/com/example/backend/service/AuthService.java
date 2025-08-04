package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
// import lombok.RequiredArgsConstructor; // 💡 삭제: 이 임포트 라인도 함께 삭제합니다.
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// @RequiredArgsConstructor // 💡 삭제: 이 어노테이션도 삭제합니다.
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 💡💡💡 수동으로 추가한 생성자는 그대로 유지합니다. 💡💡💡
    // 이 생성자가 final 필드들을 초기화하고 의존성 주입을 처리합니다.
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) { // `getEmail()` 메소드명 소문자 확인
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword()); // `getPassword()` 메소드명 소문자 확인
        User newUser = new User(request.getEmail(), encodedPassword, request.getNickname()); // `getEmail()`, `getNickname()` 메소드명 소문자 확인
        userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())); // `getEmail()`, `getPassword()` 메소드명 소문자 확인
            return jwtTokenProvider.createToken(authentication); // `createToken()` 메소드명 소문자 확인
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void changeNickname(String userEmail, String newNickname) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 닉네임 중복 체크 (선택 사항: 필요하다면 UserRepository에 existsByNickname 메소드 추가 후 사용)
        // if (userRepository.existsByNickname(newNickname)) {
        //     throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        // }

        user.setNickname(newNickname); // `setNickname()` 메소드명 소문자 확인
        userRepository.save(user); // 명시적 저장 (트랜잭션 종료 시 변경 감지하여 자동 반영될 수 있으나, 안전하게)
    }

    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) { // `getPassword()` 메소드명 소문자 확인
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("새 비밀번호는 최소 6자 이상이어야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword)); // `setPassword()` 메소드명 소문자 확인
        userRepository.save(user); // 명시적 저장
    }
}