package com.example.backend.repository;

import com.example.backend.entity.AdAccount;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional; // Optional 임포트 추가

public interface AdAccountRepository extends JpaRepository<AdAccount, Long> {

    // 🔴 이 부분이 다시 살아나는 부분이야!
    // 특정 accountId (Meta에서 가져온 숫자 ID)를 가진 AdAccount 엔티티를 Optional로 찾는 메서드
    // 단일 AdAccount를 가져올 때 사용됩니다.
    Optional<AdAccount> findByAccountId(String accountId); // 🔴 이 라인의 주석을 해제하거나 추가해주세요!

    // 🔴 이 부분은 방금 추가한 부분이야!
    // 특정 accountId를 가진 모든 AdAccount 엔티티 목록을 찾는 메서드
    // AdSyncService에서 여러 AdAccount가 같은 accountId를 가질 수 있다는 가정에 따라 필요해.
    List<AdAccount> findAllByAccountId(String accountId);

    // (기존에 있을 수 있는 다른 메서드들 - 예시)
    List<AdAccount> findByUser(User user);
    Optional<AdAccount> findByAccountIdAndUser(String accountId, User user);
}