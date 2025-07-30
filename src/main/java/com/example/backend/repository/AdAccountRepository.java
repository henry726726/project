package com.example.backend.repository;

import com.example.backend.entity.AdAccount;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdAccountRepository extends JpaRepository<AdAccount, String> {
    Optional<AdAccount> findByAccountId(String id);

    // ✅ 특정 사용자에 연결된 광고 계정 조회용
    List<AdAccount> findByUser(User user);
}