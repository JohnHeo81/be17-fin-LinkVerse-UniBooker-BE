package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 로그인 시도 제한 서비스
 * - 5회 연속 실패 시 15분 잠금
 * - Redis 기반 실패 횟수 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 접두사 */
    private static final String KEY_PREFIX = "login:fail:";

    /** 최대 실패 허용 횟수 */
    private static final int MAX_ATTEMPTS = 5;

    /** 잠금 시간 (분) */
    private static final int LOCK_DURATION_MINUTES = 15;

    /**
     * 로그인 실패 기록
     */
    public void recordFailure(String email) {
        String key = KEY_PREFIX + email;
        String currentValue = redisTemplate.opsForValue().get(key);

        int attempts = (currentValue != null) ? Integer.parseInt(currentValue) + 1 : 1;

        redisTemplate.opsForValue().set(key, String.valueOf(attempts), LOCK_DURATION_MINUTES, TimeUnit.MINUTES);

        log.info("로그인 실패 기록 - email: {}, 시도 횟수: {}/{}", email, attempts, MAX_ATTEMPTS);
    }

    /**
     * 로그인 성공 시 실패 횟수 초기화
     */
    public void resetAttempts(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
        log.info("로그인 성공 - 실패 횟수 초기화: {}", email);
    }

    /**
     * 계정 잠금 여부 확인
     */
    public boolean isLocked(String email) {
        String key = KEY_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return false;
        }

        int attempts = Integer.parseInt(value);
        return attempts >= MAX_ATTEMPTS;
    }

    /**
     * 현재 실패 횟수 조회
     */
    public int getFailedAttempts(String email) {
        String key = KEY_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        return (value != null) ? Integer.parseInt(value) : 0;
    }

    /**
     * 잠금 해제까지 남은 시간 (초)
     */
    public long getRemainingLockTime(String email) {
        String key = KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }
}