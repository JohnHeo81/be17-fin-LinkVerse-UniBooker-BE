package org.example.unibooker.domain.reservation.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Hold(임시 점유) DTO
 * - 시간/좌석 선택 시 임시 점유
 */
@Schema(description = "Hold(임시 점유) 관련 DTO")
public class HoldDto {

    /**
     * Hold 생성 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hold 생성 요청")
    public static class Request {

        @NotNull(message = "예약 날짜는 필수입니다.")
        @Schema(description = "예약 날짜", example = "2025-10-16")
        private LocalDate date;

        @NotNull(message = "예약 시간은 필수입니다.")
        @Schema(description = "예약 시간", example = "10:00")
        private LocalTime time;

        @Min(value = 1, message = "좌석 행은 1 이상이어야 합니다.")
        @Schema(description = "좌석 행 (좌석형만)", example = "3")
        private Integer row;

        @Min(value = 1, message = "좌석 열은 1 이상이어야 합니다.")
        @Schema(description = "좌석 열 (좌석형만)", example = "5")
        private Integer col;

        @Positive(message = "TTL은 양수여야 합니다.")
        @Schema(description = "페이지 TTL (초)", example = "300")
        private Long ttl;
    }

    /**
     * Hold 생성 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hold 생성 응답")
    public static class Response {

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "안내 메시지", example = "임시 점유가 완료되었습니다.")
        private String message;

        @Schema(description = "남은 점유 시간 (초)", example = "300")
        private Long remainingSeconds;
    }

    /**
     * Hold 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hold 상세 정보")
    public static class HoldInfo {

        @Schema(description = "예약 날짜", example = "2025-10-16")
        private LocalDate date;

        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "예약 시간", example = "10:00")
        private LocalTime time;

        @Schema(description = "좌석 행", example = "3")
        private Integer row;

        @Schema(description = "좌석 열", example = "5")
        private Integer col;

        @Schema(description = "점유자 ID", example = "1")
        private Long holderId;

        @Schema(description = "남은 점유 시간 (초)", example = "180")
        private Long remainingSeconds;
    }

    /**
     * Hold 목록 조회 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Hold 목록 조회 응답")
    public static class StatusResponse {

        @Schema(description = "Hold 목록")
        private List<HoldInfo> holds;

        @Schema(description = "전체 Hold 수", example = "5")
        private int totalCount;
    }
}