package org.example.unibooker.domain.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.queue.model.dto.QueueDto;
import org.example.unibooker.domain.resource.model.entity.ServiceCategory;
import org.example.unibooker.domain.resource.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 대기열 서비스
 * - Redis ZSet 기반 순번 관리
 * - 대기 토큰 TTL 5분 (Polling마다 갱신)
 * - 입장 토큰 TTL 3분 (갱신 안됨)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final RedisTemplate<String, String> redisTemplate;

    private final ResourceRepository resourceRepository;

    /** 대기열 키 접두사 */
    private static final String QUEUE_KEY_PREFIX = "queue:resource:";

    /** 대기 토큰 키 접두사 */
    private static final String WAIT_TOKEN_PREFIX = "queue:wait:";

    /** 입장 토큰 키 접두사 */
    private static final String ENTER_TOKEN_PREFIX = "queue:enter:";

    /** 대기 토큰 TTL (초) - application.yml에서 주입 */
    @Value("${ttl.queue.wait}")
    private long waitTokenTtl;

    /** 입장 토큰 TTL (초) - application.yml에서 주입 */
    @Value("${ttl.queue.enter}")
    private long enterTokenTtl;

    /** 예상 처리 시간 (초/명) */
    private static final long ESTIMATED_TIME_PER_USER = 30;

    /** 타입별 최대 슬롯 수 */
    private static final int SLOTS_RESERVATION = 5;
    private static final int SLOTS_SEAT = 10;
    private static final int SLOTS_EVENT = 10;
    private static final int SLOTS_DEFAULT = 5;

    /**
     * 대기열 진입
     * - 이미 대기 중이면 기존 토큰 반환
     * - 신규면 토큰 발급
     */
    public QueueDto.JoinResponse joinQueue(Long resourceId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + resourceId;
        String userQueueKey = "queue:user:" + resourceId + ":" + userId;

        // 이미 대기 중인지 확인
        String existingToken = redisTemplate.opsForValue().get(userQueueKey);
        if (existingToken != null) {
            // 기존 토큰의 순번 조회
            Long position = redisTemplate.opsForZSet().rank(queueKey, existingToken);
            Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);

            log.info("[Queue] 이미 대기 중 - resourceId: {}, userId: {}, token: {}",
                    resourceId, userId, existingToken);

            return QueueDto.JoinResponse.builder()
                    .token(existingToken)
                    .position(position != null ? position + 1 : 1L)
                    .totalWaiting(totalWaiting != null ? totalWaiting : 1L)
                    .message("이미 대기열에 등록되어 있습니다.")
                    .build();
        }

        // 신규 토큰 발급
        String token = UUID.randomUUID().toString();
        String waitTokenKey = WAIT_TOKEN_PREFIX + token;

        double score = System.currentTimeMillis();

        // ZSet에 토큰 추가
        redisTemplate.opsForZSet().add(queueKey, token, score);

        // 대기 토큰 저장 (TTL 5분)
        String tokenValue = String.format("%d:%d", userId, resourceId);
        redisTemplate.opsForValue().set(waitTokenKey, tokenValue, waitTokenTtl, TimeUnit.SECONDS);

        // 사용자별 대기 토큰 저장 (중복 방지용, TTL 5분)
        redisTemplate.opsForValue().set(userQueueKey, token, waitTokenTtl, TimeUnit.SECONDS);

        Long position = redisTemplate.opsForZSet().rank(queueKey, token);
        Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);

        log.info("[Queue] 대기열 진입 - resourceId: {}, userId: {}, token: {}, position: {}",
                resourceId, userId, token, position + 1);

        return QueueDto.JoinResponse.builder()
                .token(token)
                .position(position + 1)
                .totalWaiting(totalWaiting)
                .message("대기열에 등록되었습니다.")
                .build();
    }

    /**
     * 대기열 상태 조회 (Polling)
     * - 현재 순번 반환
     * - TTL 갱신
     */
    public QueueDto.StatusResponse getQueueStatus(Long resourceId, String token) {
        String queueKey = QUEUE_KEY_PREFIX + resourceId;
        String waitTokenKey = WAIT_TOKEN_PREFIX + token;

        // 토큰 유효성 확인
        String tokenValue = redisTemplate.opsForValue().get(waitTokenKey);
        if (tokenValue == null) {
            log.warn("[Queue] 만료된 토큰 - token: {}", token);
            return QueueDto.StatusResponse.builder()
                    .position(-1L)
                    .canEnter(false)
                    .message("토큰이 만료되었습니다. 다시 대기열에 등록해주세요.")
                    .build();
        }

        // TTL 갱신 (5분)
        redisTemplate.expire(waitTokenKey, waitTokenTtl, TimeUnit.SECONDS);

        // 현재 순번 조회
        Long position = redisTemplate.opsForZSet().rank(queueKey, token);
        if (position == null) {
            return QueueDto.StatusResponse.builder()
                    .position(-1L)
                    .canEnter(false)
                    .message("대기열에서 찾을 수 없습니다.")
                    .build();
        }

        Long totalWaiting = redisTemplate.opsForZSet().size(queueKey);
        Long etaSeconds = position * ESTIMATED_TIME_PER_USER;

        // 타입별 슬롯 수 계산
        ServiceCategory category = getResourceCategory(resourceId);
        int maxSlots = getMaxSlots(category);
        long activeEnterTokens = countActiveEnterTokens(resourceId);
        long availableSlots = maxSlots - activeEnterTokens;

// 입장 가능 여부 (순번이 빈 슬롯 수보다 작으면 입장 가능)
        boolean canEnter = (position < availableSlots);

        log.debug("[Queue] 슬롯 현황 - maxSlots: {}, active: {}, available: {}, position: {}, canEnter: {}",
                maxSlots, activeEnterTokens, availableSlots, position, canEnter);

        log.debug("[Queue] 상태 조회 - token: {}, position: {}, canEnter: {}",
                token, position + 1, canEnter);

        return QueueDto.StatusResponse.builder()
                .position(position + 1)  // 0-based → 1-based
                .totalWaiting(totalWaiting)
                .etaSeconds(etaSeconds)
                .canEnter(canEnter)
                .message(canEnter ? "입장 가능합니다." : "대기 중입니다.")
                .build();
    }

    /**
     * 토큰 소비 (입장)
     * - 대기열에서 제거
     * - 입장 토큰 발급 (TTL 3분)
     */
    public QueueDto.ConsumeResponse consumeToken(Long resourceId, String token) {
        String queueKey = QUEUE_KEY_PREFIX + resourceId;
        String waitTokenKey = WAIT_TOKEN_PREFIX + token;
        String enterTokenKey = ENTER_TOKEN_PREFIX + token;

        // 토큰 유효성 확인
        String tokenValue = redisTemplate.opsForValue().get(waitTokenKey);
        if (tokenValue == null) {
            log.warn("[Queue] 만료된 토큰으로 입장 시도 - token: {}", token);
            return QueueDto.ConsumeResponse.builder()
                    .success(false)
                    .message("토큰이 만료되었습니다.")
                    .build();
        }

        // tokenValue에서 userId 추출하여 사용자별 대기 키 삭제
        String[] parts = tokenValue.split(":");
        if (parts.length >= 2) {
            String userQueueKey = "queue:user:" + resourceId + ":" + parts[0];
            redisTemplate.delete(userQueueKey);
        }

        // 대기열에서 제거
        redisTemplate.opsForZSet().remove(queueKey, token);

        // 대기 토큰 삭제
        redisTemplate.delete(waitTokenKey);

        // 입장 토큰 발급 (TTL 3분, 갱신 안됨)
        redisTemplate.opsForValue().set(enterTokenKey, tokenValue, enterTokenTtl, TimeUnit.SECONDS);

        log.info("[Queue] 입장 완료 - token: {}, 남은 시간: {}초", token, enterTokenTtl);

        return QueueDto.ConsumeResponse.builder()
                .success(true)
                .message("입장이 완료되었습니다. " + (enterTokenTtl / 60) + "분 내에 예약을 완료해주세요.")
                .remainingSeconds(enterTokenTtl)
                .build();
    }

    /**
     * 대기열 이탈
     */
    public void leaveQueue(Long resourceId, String token) {
        String queueKey = QUEUE_KEY_PREFIX + resourceId;
        String waitTokenKey = WAIT_TOKEN_PREFIX + token;

        // tokenValue에서 userId 추출하여 사용자별 대기 키 삭제
        String tokenValue = redisTemplate.opsForValue().get(waitTokenKey);
        if (tokenValue != null) {
            String[] parts = tokenValue.split(":");
            if (parts.length >= 2) {
                String userQueueKey = "queue:user:" + resourceId + ":" + parts[0];
                redisTemplate.delete(userQueueKey);
            }
        }

        redisTemplate.opsForZSet().remove(queueKey, token);
        redisTemplate.delete(waitTokenKey);

        log.info("[Queue] 대기열 이탈 - resourceId: {}, token: {}", resourceId, token);
    }

    /**
     * 입장 토큰 유효성 확인
     */
    public boolean isValidEnterToken(String token) {
        String enterTokenKey = ENTER_TOKEN_PREFIX + token;
        return redisTemplate.hasKey(enterTokenKey);
    }

    /**
     * 리소스 카테고리 조회
     */
    private ServiceCategory getResourceCategory(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .map(resource -> resource.getResourceGroup().getCategory())
                .orElse(null);
    }

    /**
     * 타입별 최대 슬롯 수 반환
     */
    private int getMaxSlots(ServiceCategory category) {
        if (category == null) return SLOTS_DEFAULT;

        return switch (category) {
            case RESERVATION -> SLOTS_RESERVATION;
            case SEAT -> SLOTS_SEAT;
            case EVENT -> SLOTS_EVENT;
            default -> SLOTS_DEFAULT;
        };
    }

    /**
     * 현재 활성 입장 토큰 수 카운팅
     */
    private long countActiveEnterTokens(Long resourceId) {
        String pattern = ENTER_TOKEN_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        long count = 0;
        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && value.endsWith(":" + resourceId)) {
                count++;
            }
        }

        return count;
    }
}