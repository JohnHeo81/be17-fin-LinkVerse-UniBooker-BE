package org.example.unibooker.domain.reservation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Hold 상태 변경 WebSocket 브로드캐스트 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldWebSocketDto {

    /** 이벤트 타입 */
    private EventType type;

    /** 리소스 ID */
    private Long resourceId;

    /** 날짜 (yyyy-MM-dd) */
    private String date;

    /** 시간 (HH:mm) */
    private String time;

    /** 좌석 행 (SEAT 타입만) */
    private Integer row;

    /** 좌석 열 (SEAT 타입만) */
    private Integer col;

    /** 이벤트 타입 enum */
    public enum EventType {
        HOLD_CREATED,           // Hold 생성
        HOLD_RELEASED,          // Hold 해제
        RESERVATION_COMPLETED   // 예약 완료
    }
}