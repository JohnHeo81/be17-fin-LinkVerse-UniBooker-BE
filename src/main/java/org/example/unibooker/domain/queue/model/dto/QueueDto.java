package org.example.unibooker.domain.queue.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기열 관련 DTO
 * - 대기열 진입, 상태 조회, 입장 응답
 */
@Schema(description = "대기열 관련 DTO")
public class QueueDto {

    /**
     * 대기열 진입 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대기열 진입 응답")
    public static class JoinResponse {

        @Schema(description = "대기열 토큰", example = "abc123-token")
        private String token;

        @Schema(description = "현재 대기 순번", example = "5")
        private Long position;

        @Schema(description = "전체 대기 인원", example = "20")
        private Long totalWaiting;

        @Schema(description = "안내 메시지", example = "대기열에 등록되었습니다.")
        private String message;
    }

    /**
     * 대기열 상태 조회 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대기열 상태 조회 응답")
    public static class StatusResponse {

        @Schema(description = "현재 대기 순번", example = "3")
        private Long position;

        @Schema(description = "전체 대기 인원", example = "15")
        private Long totalWaiting;

        @Schema(description = "예상 대기 시간 (초)", example = "120")
        private Long etaSeconds;

        @Schema(description = "입장 가능 여부", example = "false")
        private Boolean canEnter;

        @Schema(description = "안내 메시지", example = "약 2분 후 입장 가능합니다.")
        private String message;
    }

    /**
     * 토큰 소비(입장) 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "토큰 소비(입장) 응답")
    public static class ConsumeResponse {

        @Schema(description = "입장 성공 여부", example = "true")
        private Boolean success;

        @Schema(description = "안내 메시지", example = "입장이 완료되었습니다.")
        private String message;

        @Schema(description = "세션 남은 시간 (초)", example = "300")
        private Long remainingSeconds;
    }
}
