package org.example.unibooker.query.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "리소스 예약 복합 조회 DTO")
public class ResourceReservationDto {

    /**
     * 리소스별 예약 현황 응답
     */
    @Getter
    @Builder
    @Schema(description = "리소스별 예약 현황 응답")
    public static class ResourceReservationCountRes {

        @Schema(description = "리소스 ID", example = "1")
        private Long resourceId;

        @Schema(description = "리소스명", example = "회의실 A")
        private String resourceName;

        @Schema(description = "타임슬롯별 예약 현황")
        private List<SlotCount> slots;

        /**
         * 타임슬롯별 예약 수
         */
        @Getter
        @Builder
        @Schema(description = "타임슬롯별 예약 수")
        public static class SlotCount {

            @Schema(description = "시작 일시", example = "2025-10-16T10:00:00")
            private LocalDateTime startAt;

            @Schema(description = "종료 일시", example = "2025-10-16T11:00:00")
            private LocalDateTime endAt;

            @Schema(description = "예약 수", example = "3")
            private int count;
        }
    }
}
