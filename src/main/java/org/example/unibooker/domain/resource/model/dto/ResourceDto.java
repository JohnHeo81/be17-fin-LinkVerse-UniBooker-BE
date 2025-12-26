package org.example.unibooker.domain.resource.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.unibooker.domain.resource.model.entity.ResourceGroups;
import org.example.unibooker.domain.resource.model.entity.ResourceStatus;
import org.example.unibooker.domain.resource.model.entity.Resources;
import org.example.unibooker.domain.resource.model.entity.ServiceCategory;
import org.example.unibooker.domain.user.model.entity.Users;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "서비스 관련 DTO 클래스들")
public class ResourceDto {

    @Getter
    @Builder
    @Schema(description = "서비스 생성 요청 DTO")
    public static class ResourceRegisterReq {

        @NotBlank(message = "서비스 이름은 필수입니다.")
        @Size(max = 100, message = "서비스 이름은 100자 이하여야 합니다.")
        @Schema(description = "서비스 이름", example = "회의실 101")
        private String name;

        @Size(max = 500, message = "서비스 설명은 500자 이하여야 합니다.")
        @Schema(description = "서비스 설명", example = "회의실 101 예약용")
        private String description;

        @NotNull(message = "서비스 그룹 ID는 필수입니다.")
        @Schema(description = "서비스가 속한 그룹 ID", example = "1")
        private Long resourceGroupId;

        @Schema(description = "서비스 이미지 URL", example = "https://example.com/img1.jpg")
        private String resourceImage;

        @Schema(description = "시작 날짜", example = "2025.10.16", nullable = true)
        private LocalDate startDate;

        @Schema(description = "종료 날짜", example = "2025.10.18", nullable = true)
        private LocalDate endDate;

        @Schema(description = "시간 간격", example = "30 또는 60", nullable = true)
        private Integer timeInterval;

        @Positive(message = "인원수는 1명 이상이어야 합니다.")
        @Schema(description = "인원수", example = "4", nullable = true)
        private Integer capacity;

        @Positive(message = "행은 1 이상이어야 합니다.")
        @Schema(description = "행", example = "4", nullable = true)
        private Integer row;

        @Positive(message = "열은 1 이상이어야 합니다.")
        @Schema(description = "열", example = "4", nullable = true)
        private Integer col;

        @Schema(description = "커스텀 필드 값 목록 (RESOURCE 타입)", nullable = true)
        private List<CustomFieldDto.CustomFieldValue> customFieldValues;

        @Schema(description = "타임슬롯 목록", nullable = true)
        private List<TimeSlotDto.TimeSlotRequest> timeSlots;

        @Schema(description = "예외 타임슬롯 목록", nullable = true)
        private List<TimeSlotDto.ExceptionSlotRequest> exceptionSlots;

        // 입력값 검증 함수
        public void validate() {
            // 종료일 체크
            if (endDate != null && endDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("종료일이 오늘 이전인 리소스는 생성할 수 없습니다.");
            }
            // 시작일/종료일 체크
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
            }
            // 시간 간격 검증
            if (timeInterval != null && timeInterval != 30 && timeInterval != 60) {
                throw new IllegalArgumentException("timeInterval은 30 또는 60만 가능합니다.");
            }
        }

        public Resources toEntity(ResourceGroups group, Users authUser) {
            LocalDate today = LocalDate.now();

            // status 결정
            ResourceStatus status;
            boolean alwaysAvailable = Boolean.TRUE.equals(group.getIsAlwaysAvailable());
            boolean startDatePassed = startDate != null && !startDate.isAfter(today);

            if (alwaysAvailable || startDatePassed) {
                status = ResourceStatus.IN_PROGRESS; // 진행 중
            } else {
                status = ResourceStatus.PROGRESS_BEFORE; // 진행 전
            }

            // Entity 생성
            return Resources.builder()
                    .name(name)
                    .description(description)
                    .resourceImage(resourceImage)
                    .resourceGroup(group)
                    .startDate(startDate)
                    .endDate(endDate)
                    .timeInterval(timeInterval)
                    .capacity(capacity)
                    .row(row)
                    .col(col)
                    .status(status)
                    .createdBy(authUser)
                    .updatedBy(authUser)
                    .build();
        }
    }


    @Getter
    @Builder
    @Schema(description = "서비스 상세 조회 응답 DTO (리소스 수정용 & 상세 조회용)")
    public static class ResourceDetailInfo {

        @Schema(description = "서비스 그룹 아이디", example = "1")
        private Long resourceGroupId;

        @Schema(description = "서비스 그룹명", example = "회의실 예약")
        private String resourceGroupName;

        @Schema(description = "서비스 아이디", example = "1")
        private Long id;

        @Schema(description = "서비스 이름", example = "회의실 101")
        private String name;

        @Schema(description = "서비스 설명", example = "회의실 101 예약용")
        private String description;

        @Schema(description = "서비스 이미지 URL", example = "https://example.com/img1.jpg")
        private String resourceImage;

        @Schema(description = "서비스 상태", example = "PROGRESS_BEFORE/PROGRESS_BEFORE/CLOSE")
        private ResourceStatus status;

        @Schema(description = "생성자 이름", example = "김한화")
        private String createdByName;

        @Schema(description = "업데이트 날짜", example = "2025.10.20")
        private String updatedAt;

        @Schema(description = "시작 날짜", example = "2025.10.16", nullable = true)
        private LocalDate startDate;

        @Schema(description = "종료 날짜", example = "2025.10.18", nullable = true)
        private LocalDate endDate;

        @Schema(description = "시간 간격", example = "30 또는 60", nullable = true)
        private int timeInterval;

        @Schema(description = "인원수", example = "4", nullable = true)
        private Integer capacity;

        @Schema(description = "행", example = "4", nullable = true)
        private Integer row;

        @Schema(description = "열", example = "4", nullable = true)
        private Integer col;

        @Schema(description = "서비스 카테고리", example = "RESERVATION(예약형)/SEAT(좌석형)/EVENT(신청형)")
        private ServiceCategory category;

        @Schema(description = "상시 모집 여부", example = "true")
        private Boolean isAlwaysAvailable;

        @Schema(description = "수정 버전", example = "5")
        private Long version;

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        public static ResourceDetailInfo fromEntity(Resources resource) {
            return new ResourceDetailInfo(
                    resource.getResourceGroup() != null ? resource.getResourceGroup().getId() : null,
                    resource.getResourceGroup() != null ? resource.getResourceGroup().getName() : null,
                    resource.getId(),
                    resource.getName(),
                    resource.getDescription(),
                    resource.getResourceImage(),
                    resource.getStatus(),
                    resource.getCreatedBy() != null ? resource.getCreatedBy().getName() : null,
                    resource.getUpdatedAt() != null ? resource.getUpdatedAt().format(DATE_FORMATTER) : null,
                    resource.getStartDate(),
                    resource.getEndDate(),
                    resource.getTimeInterval(),
                    resource.getCapacity() != null ? resource.getCapacity() : null,
                    resource.getRow(),
                    resource.getCol(),
                    resource.getResourceGroup() != null ? resource.getResourceGroup().getCategory() : null,
                    resource.getResourceGroup() != null ? resource.getResourceGroup().getIsAlwaysAvailable() : null,
                    resource.getVersion()
            );
        }
    }


    @Getter
    @Builder
    @Schema(description = "리소스 목록 조회 응답 DTO")
    public static class ResourceListRes {

        @Schema(description = "리소스 목록")
        private List<ResourceDetailInfo> resources;

        public static ResourceListRes fromEntity(List<ResourceDetailInfo> resourceList) {
            return ResourceListRes.builder()
                    .resources(resourceList)
                    .build();
        }
    }


    @Getter
    @Builder
    @Schema(description = "리소스 수정 요청 DTO")
    public static class ResourceUpdateReq {

        @Size(max = 100, message = "서비스 이름은 100자 이하여야 합니다.")
        @Schema(description = "서비스 이름", example = "회의실 101")
        private String name;

        @Size(max = 500, message = "서비스 설명은 500자 이하여야 합니다.")
        @Schema(description = "서비스 설명", example = "회의실 101 예약용")
        private String description;

        @Schema(description = "서비스 이미지 URL", example = "https://example.com/img1.jpg")
        private String resourceImage;

        @Schema(description = "시작 날짜", example = "2025-10-16", nullable = true)
        private LocalDate startDate;

        @Schema(description = "종료 날짜", example = "2025-10-18", nullable = true)
        private LocalDate endDate;

        @Schema(description = "시작 시간", example = "12:00", nullable = true)
        private LocalTime startTime;

        @Schema(description = "종료 시간", example = "19:00", nullable = true)
        private LocalTime endTime;

        @Schema(description = "시간 간격", example = "30 또는 60", nullable = true)
        private Integer timeInterval;

        @Positive(message = "인원수는 1명 이상이어야 합니다.")
        @Schema(description = "인원수", example = "4", nullable = true)
        private Integer capacity;

        @Positive(message = "행은 1 이상이어야 합니다.")
        @Schema(description = "행", example = "4", nullable = true)
        private Integer row;

        @Positive(message = "열은 1 이상이어야 합니다.")
        @Schema(description = "열", example = "4", nullable = true)
        private Integer col;

        @Schema(description = "타임슬롯 목록", nullable = true)
        private List<TimeSlotDto.TimeSlotRequest> timeSlots;

        @Schema(description = "예외 타임슬롯 목록", nullable = true)
        private List<TimeSlotDto.ExceptionSlotRequest> exceptionSlots;

        @Schema(description = "예약 처리 액션 (CANCEL: 취소, MODIFY: 수정)", example = "CANCEL", nullable = true)
        private String reservationAction;
    }

    @Getter
    @Setter
    @Schema(description = "서비스 상태 변경 요청 DTO")
    public static class ResourceStatusChangeReq {  // 오타 수정: ChangReq → ChangeReq

        @NotNull(message = "리소스 ID는 필수입니다.")
        @Schema(description = "리소스 ID", example = "1")
        private Long resourceId;

        @NotNull(message = "버전은 필수입니다.")
        @Schema(description = "버전", example = "1")
        private Long version;

        @NotBlank(message = "변경할 상태는 필수입니다.")
        @Schema(description = "변경할 상태", example = "IN_PROGRESS")
        private String targetStatus;
    }


    @Getter
    @Builder
    @Schema(description = "서비스 삭제 요청 DTO")
    public static class ResourceGroupDeleteReq {

        @Schema(description = "삭제할 서비스 그룹 ID", example = "1")
        private Long id;
    }
}