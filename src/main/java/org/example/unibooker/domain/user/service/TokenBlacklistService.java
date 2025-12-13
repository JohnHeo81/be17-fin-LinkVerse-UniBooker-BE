package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 토큰 블랙리스트 서비스
 * - 로그아웃 시 Access Token 무효화
 * - Redis 기반 블랙리스트 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 접두사 */
    private static final String KEY_PREFIX = "blacklist:";

    /**
     * 토큰을 블랙리스트에 추가
     * @param token Access Token
     * @param remainingTimeMillis 토큰 남은 만료 시간 (ms)
     */
    public void addToBlacklist(String token, long remainingTimeMillis) {
        if (remainingTimeMillis <= 0) {
            log.debug("이미 만료된 토큰 - 블랙리스트 추가 생략");
            return;
        }

        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", remainingTimeMillis, TimeUnit.MILLISECONDS);
        log.info("토큰 블랙리스트 추가 - TTL: {}ms", remainingTimeMillis);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        String key = KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 블랙리스트에서 토큰 제거 (테스트용)
     */
    public void removeFromBlacklist(String token) {
        String key = KEY_PREFIX + token;
        redisTemplate.delete(key);
        log.info("토큰 블랙리스트 제거");
    }
}