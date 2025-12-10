package org.example.unibooker.domain.reservation.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class HoldDto {

    /**
     * Hold 생성 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private LocalDate date;
        private LocalTime time;
        private Integer row;
        private Integer col;
        private Long ttl;  // 남은 페이지 TTL (초)
    }

    /**
     * Hold 생성 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean success;
        private String message;
        private Long remainingSeconds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldInfo {
        private LocalDate date;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;

        private Integer row;
        private Integer col;
        private Long holderId;
        private Long remainingSeconds;
    }

    /**
     * Hold 목록 조회 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private List<HoldInfo> holds;
        private int totalCount;
    }
}