package org.example.unibooker.domain.reservation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Hold 상태 변경 WebSocket 메시지")
public class HoldWebSocketDto {

    @Schema(description = "이벤트 타입", example = "HOLD_CREATED")
    private EventType type;

    @Schema(description = "리소스 ID", example = "1")
    private Long resourceId;

    @Schema(description = "날짜 (yyyy-MM-dd)", example = "2025-10-16")
    private String date;

    @Schema(description = "시간 (HH:mm)", example = "10:00")
    private String time;

    @Schema(description = "좌석 행 (SEAT 타입만)", example = "3")
    private Integer row;

    @Schema(description = "좌석 열 (SEAT 타입만)", example = "5")
    private Integer col;

    /**
     * 이벤트 타입 enum
     */
    @Schema(description = "Hold 이벤트 타입")
    public enum EventType {
        @Schema(description = "Hold 생성")
        HOLD_CREATED,

        @Schema(description = "Hold 해제")
        HOLD_RELEASED,

        @Schema(description = "예약 완료")
        RESERVATION_COMPLETED
    }
}