package com.example.backend.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class BlacklistedTokenService {

    // 토큰 (key)과 만료 시간 (value)을 저장할 Map
    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BlacklistedTokenService() {
        // 주기적으로 만료된 토큰을 제거하는 스케줄러 (1분마다 실행)
        scheduler.scheduleAtFixedRate(this::removeExpiredTokens, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 토큰을 블랙리스트에 추가합니다.
     * @param token 블랙리스트에 추가할 JWT 토큰
     * @param expiration 만료 시간 (Instant 객체)
     */
    public void blacklistToken(String token, Instant expiration) {
        blacklistedTokens.put(token, expiration);
    }

    /**
     * 주어진 토큰이 블랙리스트에 있는지 확인합니다.
     * @param token 확인할 JWT 토큰
     * @return 블랙리스트에 있고 아직 만료되지 않았다면 true, 아니면 false
     */
    public boolean isTokenBlacklisted(String token) {
        Instant expiration = blacklistedTokens.get(token);
        return expiration != null && expiration.isAfter(Instant.now());
    }

    /**
     * 만료된 토큰들을 블랙리스트에서 제거합니다. (내부용)
     */
    private void removeExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.forEach((token, expiration) -> {
            if (expiration.isBefore(now)) {
                blacklistedTokens.remove(token);
            }
        });
    }

    // 애플리케이션 종료 시 스케줄러 종료
    // (Bean Lifecycle 관리를 위해 @PreDestroy 어노테이션 사용도 고려 가능)
    public void destroy() {
        scheduler.shutdownNow();
    }
}