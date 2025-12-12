package org.example.unibooker.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.reservation.model.dto.HoldDto;
import org.example.unibooker.domain.resource.model.entity.Resources;
import org.example.unibooker.domain.resource.model.entity.ServiceCategory;
import org.example.unibooker.domain.resource.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;

/**
 * Hold(임시 점유) 서비스
 * - 시간/좌석 선택 시 2분간 임시 점유
 * - 다른 사용자 선택 차단
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ResourceRepository resourceRepository;
    private final HoldWebSocketService holdWebSocketService;

    /** Hold 키 접두사 */
    private static final String HOLD_KEY_PREFIX = "hold:";

    /** Hold TTL (초) - application.yml에서 주입 */
    @Value("${ttl.hold}")
    private long holdTtl;

    /**
     * Hold 생성
     */
    public HoldDto.Response createHold(Long resourceId, Long userId, HoldDto.Request request) {
        // 리소스 조회
        Resources resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.RESOURCE_NOT_FOUND));

        ServiceCategory category = resource.getResourceGroup().getCategory();

        // Hold 키 생성
        String holdKey = generateHoldKey(resourceId, category, request);

        // 이미 Hold가 있는지 확인
        String existingHolder = redisTemplate.opsForValue().get(holdKey);

        if (existingHolder != null) {
            Long holderId = Long.parseLong(existingHolder);

            // 본인이 이미 Hold 중이면 TTL 갱신
            if (holderId.equals(userId)) {
                redisTemplate.expire(holdKey, holdTtl, TimeUnit.SECONDS);
                log.info("[Hold] TTL 갱신 - key: {}, userId: {}", holdKey, userId);

                return HoldDto.Response.builder()
                        .success(true)
                        .message("선택이 유지됩니다.")
                        .remainingSeconds(holdTtl)
                        .build();
            }

            // 다른 사용자가 Hold 중
            log.warn("[Hold] 이미 선택됨 - key: {}, holder: {}, requester: {}", holdKey, holderId, userId);

            String message = category == ServiceCategory.SEAT
                    ? "해당 좌석은 다른 사용자가 선택 중입니다."
                    : "해당 시간은 다른 사용자가 선택 중입니다.";

            return HoldDto.Response.builder()
                    .success(false)
                    .message(message)
                    .build();
        }

        // Hold TTL: 백엔드 고정값 사용 (보안)
        long ttl = holdTtl;

        // Hold 생성
        redisTemplate.opsForValue().set(holdKey, userId.toString(), ttl, TimeUnit.SECONDS);

        log.info("[Hold] 생성 완료 - key: {}, userId: {}, TTL: {}초", holdKey, userId, holdTtl);

        // WebSocket 브로드캐스트
        String dateStr = request.getDate().toString();
        String timeStr = request.getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        holdWebSocketService.broadcastHoldCreated(resourceId, dateStr, timeStr, request.getRow(), request.getCol());

        return HoldDto.Response.builder()
                .success(true)
                .message("선택이 완료되었습니다.")
                .remainingSeconds(holdTtl)
                .build();
    }

    /**
     * Hold 해제
     */
    public void releaseHold(Long resourceId, Long userId, HoldDto.Request request) {
        Resources resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.RESOURCE_NOT_FOUND));

        ServiceCategory category = resource.getResourceGroup().getCategory();
        String holdKey = generateHoldKey(resourceId, category, request);

        // 본인 Hold만 해제 가능
        String existingHolder = redisTemplate.opsForValue().get(holdKey);
        if (existingHolder != null && existingHolder.equals(userId.toString())) {
            redisTemplate.delete(holdKey);
            log.info("[Hold] 해제 완료 - key: {}, userId: {}", holdKey, userId);

            // WebSocket 브로드캐스트
            String dateStr = request.getDate().toString();
            String timeStr = request.getTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            holdWebSocketService.broadcastHoldReleased(resourceId, dateStr, timeStr, request.getRow(), request.getCol());
        }
    }

    /**
     * 특정 리소스의 Hold 목록 조회
     */
    public HoldDto.StatusResponse getHoldStatus(Long resourceId, LocalDate date) {
        String pattern = HOLD_KEY_PREFIX + resourceId + ":" + date + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        List<HoldDto.HoldInfo> holds = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                if (value != null) {
                    HoldDto.HoldInfo holdInfo = parseHoldKey(key, Long.parseLong(value), ttl);
                    if (holdInfo != null) {
                        holds.add(holdInfo);
                    }
                }
            }
        }

        return HoldDto.StatusResponse.builder()
                .holds(holds)
                .totalCount(holds.size())
                .build();
    }

    /**
     * 예약 완료 시 Hold 삭제
     */
    public void clearHoldOnReservation(Long resourceId, ServiceCategory category, LocalDate date, LocalTime time, Integer row, Integer col) {
        HoldDto.Request request = HoldDto.Request.builder()
                .date(date)
                .time(time)
                .row(row)
                .col(col)
                .build();

        String holdKey = generateHoldKey(resourceId, category, request);
        redisTemplate.delete(holdKey);
        log.info("[Hold] 예약 완료로 삭제 - key: {}", holdKey);
    }

    /**
     * Hold 키 생성
     */
    private String generateHoldKey(Long resourceId, ServiceCategory category, HoldDto.Request request) {
        if (category == ServiceCategory.SEAT) {
            return String.format("%s%d:%s:%s:%d:%d",
                    HOLD_KEY_PREFIX,
                    resourceId,
                    request.getDate(),
                    request.getTime(),
                    request.getRow(),
                    request.getCol());
        } else {
            return String.format("%s%d:%s:%s",
                    HOLD_KEY_PREFIX,
                    resourceId,
                    request.getDate(),
                    request.getTime());
        }
    }

    /**
     * Hold 키 파싱
     */
    private HoldDto.HoldInfo parseHoldKey(String key, Long holderId, Long ttl) {
        try {
            // hold:resourceId:date:time 또는 hold:resourceId:date:time:row:col
            String[] parts = key.replace(HOLD_KEY_PREFIX, "").split(":");

            HoldDto.HoldInfo.HoldInfoBuilder builder = HoldDto.HoldInfo.builder()
                    .holderId(holderId)
                    .remainingSeconds(ttl);

            if (parts.length >= 3) {
                builder.date(LocalDate.parse(parts[1]))
                        .time(LocalTime.parse(parts[2]));
            }

            if (parts.length >= 5) {
                builder.row(Integer.parseInt(parts[3]))
                        .col(Integer.parseInt(parts[4]));
            }

            return builder.build();
        } catch (Exception e) {
            log.error("[Hold] 키 파싱 실패 - key: {}", key, e);
            return null;
        }
    }
}