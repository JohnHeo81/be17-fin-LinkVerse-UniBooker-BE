package org.example.unibooker.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.reservation.model.dto.HoldWebSocketDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Hold 상태 변경 WebSocket 브로드캐스트 서비스
 * - Hold 생성/해제/예약 완료 시 해당 리소스 구독자들에게 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /** 브로드캐스트 토픽 prefix */
    private static final String TOPIC_PREFIX = "/topic/hold/";

    /**
     * Hold 생성 브로드캐스트
     */
    public void broadcastHoldCreated(Long resourceId, String date, String time, Integer row, Integer col) {
        HoldWebSocketDto message = HoldWebSocketDto.builder()
                .type(HoldWebSocketDto.EventType.HOLD_CREATED)
                .resourceId(resourceId)
                .date(date)
                .time(time)
                .row(row)
                .col(col)
                .build();

        broadcast(resourceId, message);
    }

    /**
     * Hold 해제 브로드캐스트
     */
    public void broadcastHoldReleased(Long resourceId, String date, String time, Integer row, Integer col) {
        HoldWebSocketDto message = HoldWebSocketDto.builder()
                .type(HoldWebSocketDto.EventType.HOLD_RELEASED)
                .resourceId(resourceId)
                .date(date)
                .time(time)
                .row(row)
                .col(col)
                .build();

        broadcast(resourceId, message);
    }

    /**
     * 예약 완료 브로드캐스트
     */
    public void broadcastReservationCompleted(Long resourceId, String date, String time, Integer row, Integer col) {
        HoldWebSocketDto message = HoldWebSocketDto.builder()
                .type(HoldWebSocketDto.EventType.RESERVATION_COMPLETED)
                .resourceId(resourceId)
                .date(date)
                .time(time)
                .row(row)
                .col(col)
                .build();

        broadcast(resourceId, message);
    }

    /**
     * 메시지 브로드캐스트
     */
    private void broadcast(Long resourceId, HoldWebSocketDto message) {
        String destination = TOPIC_PREFIX + resourceId;
        messagingTemplate.convertAndSend(destination, message);
        log.info("[WebSocket] 브로드캐스트: {} -> {}", destination, message.getType());
    }
}