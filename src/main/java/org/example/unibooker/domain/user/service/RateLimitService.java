package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting 서비스
 * - API 호출 횟수 제한
 * - Redis 기반 요청 횟수 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 접두사 */
    private static final String KEY_PREFIX = "rate:";

    /** TTL (초) */
    private static final int WINDOW_SECONDS = 60;

    /** 제한 횟수 (기본) */
    private static final int DEFAULT_LIMIT = 100;

    /** 제한 횟수 (로그인) */
    private static final int LOGIN_LIMIT = 10;

    /** 제한 횟수 (예약) */
    private static final int RESERVATION_LIMIT = 30;

    /**
     * 요청 허용 여부 확인 및 카운트 증가
     * @return true: 허용, false: 제한 초과
     */
    public boolean isAllowed(String identifier, String endpointCategory) {
        String key = KEY_PREFIX + identifier + ":" + endpointCategory;
        int limit = getLimit(endpointCategory);

        String currentValue = redisTemplate.opsForValue().get(key);
        int currentCount = (currentValue != null) ? Integer.parseInt(currentValue) : 0;

        if (currentCount >= limit) {
            log.warn("Rate Limit 초과 - identifier: {}, category: {}, count: {}/{}",
                    identifier, endpointCategory, currentCount, limit);
            return false;
        }

        // 카운트 증가
        if (currentValue == null) {
            redisTemplate.opsForValue().set(key, "1", WINDOW_SECONDS, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }

        return true;
    }

    /**
     * 엔드포인트 카테고리별 제한 횟수 반환
     */
    private int getLimit(String endpointCategory) {
        return switch (endpointCategory) {
            case "login" -> LOGIN_LIMIT;
            case "reservation" -> RESERVATION_LIMIT;
            default -> DEFAULT_LIMIT;
        };
    }

    /**
     * 현재 요청 횟수 조회
     */
    public int getCurrentCount(String identifier, String endpointCategory) {
        String key = KEY_PREFIX + identifier + ":" + endpointCategory;
        String value = redisTemplate.opsForValue().get(key);
        return (value != null) ? Integer.parseInt(value) : 0;
    }

    /**
     * 남은 요청 횟수 조회
     */
    public int getRemainingCount(String identifier, String endpointCategory) {
        int limit = getLimit(endpointCategory);
        int current = getCurrentCount(identifier, endpointCategory);
        return Math.max(0, limit - current);
    }

    /**
     * 제한 초기화까지 남은 시간 (초)
     */
    public long getRemainingTime(String identifier, String endpointCategory) {
        String key = KEY_PREFIX + identifier + ":" + endpointCategory;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? ttl : 0;
    }
}