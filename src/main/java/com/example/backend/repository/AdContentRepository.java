package com.example.backend.repository;

import com.example.backend.entity.AdContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// AdContent 엔티티를 위한 JPA 리포지토리
@Repository
public interface AdContentRepository extends JpaRepository<AdContent, Long> {
    // JpaRepository를 상속받으면 기본적인 CRUD(생성, 조회, 업데이트, 삭제) 메서드가 자동으로 제공됩니다.
    // 필요하다면 여기에 추가적인 쿼리 메서드를 정의할 수 있습니다.
}