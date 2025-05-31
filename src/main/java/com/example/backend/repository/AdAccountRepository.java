package com.example.backend.repository;

import com.example.backend.entity.AdAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdAccountRepository extends JpaRepository<AdAccount, String> {
}