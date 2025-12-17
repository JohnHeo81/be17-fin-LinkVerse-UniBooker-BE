package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token 저장소
 * - 배포 시에도 토큰 유지
 * - 다중 서버 환경 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenStorageService implements TokenStorageService {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis Key 접두사 */
    private static final String KEY_PREFIX = "refresh:";

    /**
     * Refresh Token 저장
     */
    @Override
    public void saveRefreshToken(Long userId, String refreshToken, long ttlMillis) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Refresh Token 저장 - userId: {}, TTL: {}ms", userId, ttlMillis);
    }

    /**
     * Refresh Token 조회
     */
    @Override
    public String getRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Refresh Token 삭제 (단일)
     */
    @Override
    public void deleteRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Refresh Token 삭제 - userId: {}", userId);
    }

    /**
     * 사용자별 모든 Refresh Token 삭제
     */
    @Override
    public void deleteAllRefreshTokens(Long userId) {
        deleteRefreshToken(userId);
        log.debug("모든 Refresh Token 삭제 - userId: {}", userId);
    }

    /**
     * Refresh Token 존재 여부 확인
     */
    @Override
    public boolean existsRefreshToken(Long userId) {
        String key = KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}