package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // 이메일을 username으로 받아 DB에서 사용자 조회
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 권한 설정 (User 엔티티의 roles 필드를 사용)
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        // UserDetails 구현체 반환 (Spring Security의 User 객체 사용)
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}