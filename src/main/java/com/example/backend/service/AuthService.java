package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.dto.UserAuthDtos;
// import org.springframework.security.core.userdetails.UserDetails; // 💡 삭제: UserDetails 임포트 제거
// import org.springframework.security.core.userdetails.UserDetailsService; // 💡 삭제: UserDetailsService 임포트 제거

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    // private final UserDetailsService userDetailsService; // 💡 삭제: UserDetailsService 주입 제거

    @Transactional
    public void signup(UserAuthDtos.SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getEmail(), encodedPassword, request.getNickname());
        userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public String login(UserAuthDtos.LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            return jwtTokenProvider.createToken(authentication);
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // 💡💡💡 삭제: OAuth2 관련 generateToken 메서드 💡💡💡
    // public String generateToken(String userEmail) {
    //     UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
    //     Authentication authentication = new UsernamePasswordAuthenticationToken(
    //         userDetails,
    //         null,
    //         userDetails.getAuthorities()
    //     );
    //     return jwtTokenProvider.createToken(authentication);
    // }
}