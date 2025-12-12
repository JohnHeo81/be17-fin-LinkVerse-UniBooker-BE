package org.example.unibooker.domain.reservation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.reservation.model.entity.ReservationStatus;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.example.unibooker.domain.resource.model.dto.CustomFieldDto;
import org.example.unibooker.domain.resource.model.entity.Resources;
import org.example.unibooker.domain.resource.model.entity.ServiceCategory;
import org.example.unibooker.domain.user.model.entity.Users;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Schema(description = "예약 관련 DTO")
public class ReservationDto {

    // ===================
    // 예약 요청 DTO
    // ===================
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "예약 요청 정보")
    public static class Request {

        @NotNull(message = "예약 날짜는 필수입니다.")
        @Schema(description = "예약할 날짜", example = "2025-10-16")
        private LocalDate date;

        @NotNull(message = "예약 시간은 필수입니다.")
        @Schema(description = "예약할 시간", example = "10:00")
        private LocalTime time;

        @Positive(message = "인원수는 1명 이상이어야 합니다.")
        @Schema(description = "인원수", example = "3")
        private Integer headCount;

        @Min(value = 1, message = "좌석 행은 1 이상이어야 합니다.")
        @Schema(description = "좌석 행 (좌석형만)", example = "1")
        private Integer row;

        @Min(value = 1, message = "좌석 열은 1 이상이어야 합니다.")
        @Schema(description = "좌석 열 (좌석형만)", example = "2")
        private Integer col;

        @Schema(description = "사용자 입력 커스텀 필드 값")
        private List<CustomFieldDto.CustomFieldValue> customFieldValues;

        /** DTO → Entity 변환 */
        public Reservations toReservationEntity(Users user, Resources resource, LocalDateTime[] dates) {
            return Reservations.builder()
                    .users(user)
                    .resources(resource)
                    .createdBy(user)
                    .status(ReservationStatus.CONFIRMED)
                    .attendeeCount(headCount)
                    .startDate(dates[0])
                    .endDate(dates[1])
                    .row(row)
                    .col(col)
                    .build();
        }
    }


    // ===================
    // 예약 목록 응답 DTO
    // ===================

    // =============== 플랫폼 관리자 및 기업 관리자 용 ===============
    @Getter
    @Builder
    @Schema(description = "관리자 예약 목록 조회 응답")
    public static class ResponseList {

        @Schema(description = "예약 목록")
        private List<Object> list;

        public static ResponseList from(List<Reservations> entities, ServiceCategory serviceCategory) {
            return switch(serviceCategory) {
                case RESERVATION ->
                        ResponseList.builder()
                                .list(entities.stream().map(ReservationResponseListInfo::from).collect(Collectors.toList()))
                                .build();
                case SEAT ->
                        ResponseList.builder()
                                .list(entities.stream().map(SeatResponseListInfo::from).collect(Collectors.toList()))
                                .build();
                case EVENT ->
                        ResponseList.builder()
                                .list(entities.stream().map(EventResponseListInfo::from).collect(Collectors.toList()))
                                .build();
                default -> throw new BaseException(BaseResponseStatus.INVALID_SERVICE_CATEGORY);
            };
        }
    }

    @Getter
    @Builder
    @Schema(description = "관리자 예약 목록 조회 [예약형] 단일 응답")
    public static class ReservationResponseListInfo {

        @Schema(description = "예약 번호", example = "1")
        private Long id;

        @Schema(description = "예약자", example = "홍길동")
        private String userName;

        @Schema(description = "예약한 리소스", example = "회의실 A")
        private String resourceName;

        @Schema(description = "예약 상태", example = "CONFIRMED")
        private ReservationStatus status;

        @Schema(description = "예약 시작 일시", example = "2025-10-16T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "예약 종료 일시", example = "2025-10-16T11:00:00")
        private LocalDateTime endDate;

        public static ReservationResponseListInfo from(Reservations entity) {
            return ReservationResponseListInfo.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .resourceName(entity.getResources().getName())
                    .status(entity.getStatus())
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "관리자 예약 목록 조회 [좌석형] 단일 응답")
    public static class SeatResponseListInfo {

        @Schema(description = "예약 번호", example = "1")
        private Long id;

        @Schema(description = "예약자", example = "홍길동")
        private String userName;

        @Schema(description = "좌석 행", example = "3")
        private Integer row;

        @Schema(description = "좌석 열", example = "5")
        private Integer col;

        @Schema(description = "예약 상태", example = "CONFIRMED")
        private ReservationStatus status;

        @Schema(description = "예약 시작 일시", example = "2025-10-16T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "예약 종료 일시", example = "2025-10-16T11:00:00")
        private LocalDateTime endDate;

        public static SeatResponseListInfo from(Reservations entity) {
            return SeatResponseListInfo.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .row(entity.getRow())
                    .col(entity.getCol())
                    .status(entity.getStatus())
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "관리자 예약 목록 조회 [신청형] 단일 응답")
    public static class EventResponseListInfo {

        @Schema(description = "예약 번호", example = "1")
        private Long id;

        @Schema(description = "예약자", example = "홍길동")
        private String userName;

        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @Schema(description = "신청일", example = "2025-10-16T10:00:00")
        private LocalDateTime applicationDate;

        @Schema(description = "신청 상태", example = "CONFIRMED")
        private ReservationStatus status;

        public static EventResponseListInfo from(Reservations entity) {
            return EventResponseListInfo.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .email(entity.getUsers().getEmail())
                    .applicationDate(entity.getCreatedAt())
                    .status(entity.getStatus())
                    .build();
        }
    }

    // =============== 일반 사용자용 ===============
    @Getter
    @Builder
    @Schema(description = "일반 사용자 예약 목록 조회 응답")
    public static class UserResponseList {

        @Schema(description = "예약 목록")
        private List<UserResponse> reservations;

        public static UserResponseList from(List<Reservations> entities) {
            return UserResponseList.builder()
                    .reservations(entities.stream().map(UserResponse::from).toList())
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    @Schema(description = "일반 사용자 예약 목록 조회 단일 응답 정보")
    public static class UserResponse extends Response {
        @Schema(description = "예약 시작 일시", example = "2025-10-16T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "예약 종료 일시", example = "2025-10-16T11:00:00")
        private LocalDateTime endDate;

        public static UserResponse from(Reservations entity) {
            return UserResponse.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .status(entity.getStatus())
                    .thumbnail(entity.getResources().getResourceGroup().getThumbnail())
                    .resourceGroupName(entity.getResources().getResourceGroup().getName())
                    .resourceName(entity.getResources().getName())
                    .serviceCategory(entity.getResources().getResourceGroup().getCategory())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .deletedAt(entity.getDeletedAt())
                    // 아래부터는 일반 사용자 예약 목록 조회용 정보
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .build();
        }
    }


    // ===================
    // 예약 상세 응답 DTO
    // ===================
    @Getter
    @SuperBuilder
    @Schema(description = "예약 상세 조회 [공통] 응답")
    public abstract static class Response {

        @Schema(description = "예약 번호", example = "1")
        private Long id;

        @Schema(description = "예약자", example = "홍길동")
        private String userName;

        @Schema(description = "예약 상태", example = "CONFIRMED")
        private ReservationStatus status;

        @Schema(description = "리소스 그룹 썸네일", example = "https://example.com/thumbnail.jpg")
        private String thumbnail;

        @Schema(description = "리소스 그룹명", example = "회의실")
        private String resourceGroupName;

        @Schema(description = "리소스명", example = "회의실 A")
        private String resourceName;

        @Schema(description = "서비스 카테고리", example = "RESERVATION")
        private ServiceCategory serviceCategory;

        @Schema(description = "생성일시", example = "2025-10-16T10:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시", example = "2025-10-16T11:00:00")
        private LocalDateTime updatedAt;

        @Schema(description = "삭제일시", example = "null")
        private LocalDateTime deletedAt;
    }

    @Getter
    @SuperBuilder
    @Schema(description = "예약 상세 조회 [예약형] 응답")
    public static class ReservationResponse extends Response {

        @Schema(description = "예약 시작 일시", example = "2025-10-16T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "예약 종료 일시", example = "2025-10-16T11:00:00")
        private LocalDateTime endDate;

        @Schema(description = "인원수", example = "4")
        private Integer headCount;

        @Schema(description = "사용자 입력 커스텀 필드 값")
        private List<CustomFieldDto.CustomFieldValueListRes> customFieldValues;

        /** Entity → DTO 변환 */
        public static ReservationResponse from(Reservations entity, List<CustomFieldDto.CustomFieldValueListRes> userCustomFieldValues) {
            return ReservationResponse.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .status(entity.getStatus())
                    .thumbnail(entity.getResources().getResourceImage())
                    .resourceGroupName(entity.getResources().getResourceGroup().getName())
                    .resourceName(entity.getResources().getName())
                    .serviceCategory(entity.getResources().getResourceGroup().getCategory())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .deletedAt(entity.getDeletedAt())
                    .customFieldValues(userCustomFieldValues)
                    // 아래부터는 예약형 정보
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .headCount(entity.getAttendeeCount())
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    @Schema(description = "예약 상세 조회 [좌석형] 응답")
    public static class SeatResponse extends Response {

        @Schema(description = "예약 시작 일시", example = "2025-10-16T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "예약 종료 일시", example = "2025-10-16T11:00:00")
        private LocalDateTime endDate;

        @Schema(description = "인원수", example = "1")
        private Integer headCount;

        @Schema(description = "좌석 행", example = "3")
        private Integer row;

        @Schema(description = "좌석 열", example = "5")
        private Integer col;

        @Schema(description = "사용자 입력 커스텀 필드 값")
        private List<CustomFieldDto.CustomFieldValueListRes> customFieldValues;

        public static SeatResponse from(Reservations entity, List<CustomFieldDto.CustomFieldValueListRes> userCustomFieldValues) {
            return SeatResponse.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .status(entity.getStatus())
                    .thumbnail(entity.getResources().getResourceImage())
                    .resourceGroupName(entity.getResources().getResourceGroup().getName())
                    .resourceName(entity.getResources().getName())
                    .serviceCategory(entity.getResources().getResourceGroup().getCategory())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .deletedAt(entity.getDeletedAt())
                    .customFieldValues(userCustomFieldValues)
                    // 아래부터는 좌석형 정보
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .headCount(entity.getAttendeeCount())
                    .row(entity.getRow())
                    .col(entity.getCol())
                    .build();
        }
    }

    @Getter
    @SuperBuilder
    @Schema(description = "예약 상세 조회 [신청형] 응답")
    public static class EventResponse extends Response {

        @Schema(description = "사용자 입력 커스텀 필드 값")
        private List<CustomFieldDto.CustomFieldValueListRes> customFieldValues;

        public static EventResponse from(Reservations entity, List<CustomFieldDto.CustomFieldValueListRes> userCustomFieldValues) {
            return EventResponse.builder()
                    .id(entity.getId())
                    .userName(entity.getUsers().getName())
                    .status(entity.getStatus())
                    .thumbnail(entity.getResources().getResourceImage())
                    .resourceGroupName(entity.getResources().getResourceGroup().getName())
                    .resourceName(entity.getResources().getName())
                    .serviceCategory(entity.getResources().getResourceGroup().getCategory())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .deletedAt(entity.getDeletedAt())
                    .customFieldValues(userCustomFieldValues)
                    .build();
        }
    }
}
