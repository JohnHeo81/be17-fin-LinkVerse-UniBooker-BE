package org.example.unibooker.domain.resource.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.unibooker.domain.resource.model.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "커스텀 필드 관련 DTO 클래스들")
public class CustomFieldDto {

    // ---------------- 필드 정의 --------------------

    @Getter
    @Builder
    @Schema(description = "커스텀 필드 생성 & 수정 요청 DTO")
    public static class CustomFieldReq {

        @NotBlank(message = "필드 이름은 필수입니다.")
        @Size(max = 50, message = "필드 이름은 50자 이하여야 합니다.")
        @Schema(description = "필드 이름", example = "회의실 이름")
        private String fieldName;

        @Size(max = 200, message = "설명은 200자 이하여야 합니다.")
        @Schema(description = "설명", example = "예약하려는 회의실의 이름")
        private String description;

        @NotNull(message = "데이터 타입은 필수입니다.")
        @Schema(description = "데이터 타입", example = "STRING / NUMBER / DATE")
        private CustomDataType dataType;

        @NotNull(message = "필드 유형은 필수입니다.")
        @Schema(description = "필드 유형", example = "SERVICE / USER")
        private CustomTargetType targetType;

        @NotNull(message = "필수 여부는 필수입니다.")
        @Schema(description = "필수 여부", example = "true")
        private Boolean required;

        @Schema(description = "선택형 옵션 목록 (RADIO/CHECKBOX)", nullable = true)
        private List<String> options;

        public CustomFieldDefinitions toEntity() {
            return CustomFieldDefinitions.builder()
                    .fieldName(fieldName)
                    .description(description)
                    .dataType(dataType)
                    .targetType(targetType)
                    .isRequired(required)
                    .build();
        }
    }


    @Getter
    @Builder
    @Schema(description = "커스텀 필드 조회 응답 DTO")
    public static class CustomFieldRes {

        @Schema(description = "필드 ID", example = "1")
        private Long id;

        @Schema(description = "필드 이름", example = "회의실 이름")
        private String fieldName;

        @Schema(description = "설명", example = "예약하려는 회의실의 이름")
        private String description;

        @Schema(description = "데이터 타입", example = "STRING / NUMBER / DATE")
        private String dataType;

        @Schema(description = "필드 유형", example = "SERVICE / USER")
        private String targetType;

        @Schema(description = "필수 여부", example = "true")
        private Boolean required;

        @Setter
        @Schema(description = "선택형 옵션 목록 (RADIO/CHECKBOX)", nullable = true)
        private List<String> options;

        public static CustomFieldRes fromEntity(CustomFieldDefinitions entity) {
            // RADIO/CHECKBOX 옵션 조회
            List<String> options = null;
            if (entity.getCustomFieldSelectDefinitions() != null
                    && !entity.getCustomFieldSelectDefinitions().isEmpty()) {
                options = entity.getCustomFieldSelectDefinitions().stream()
                        .map(CustomFieldSelectDefinitions::getName)
                        .collect(Collectors.toList());
            }

            return CustomFieldRes.builder()
                    .id(entity.getId())
                    .fieldName(entity.getFieldName())
                    .dataType(entity.getDataType().name())
                    .targetType(entity.getTargetType().name())
                    .required(entity.getIsRequired())
                    .description(entity.getDescription())
                    .options(options)
                    .build();
        }
    }



    // ---------------- 필드 값 --------------------

    @Getter
    @Builder
    @Schema(description = "커스텀 필드 값 생성 DTO")
    public static class CustomFieldValue {

        @Schema(description = "필드 ID", example = "1")
        private Long customFieldId;

        @Schema(description = "입력 값", example = "회의실 101")
        private List<String> values;

        public List<UserCustomFieldValues> toUserEntity(CustomFieldDefinitions field, Long reservationId) {
            List<UserCustomFieldValues> entities = new ArrayList<>();
            for (String v : values) {
                entities.add(UserCustomFieldValues.builder()
                        .reservationId(reservationId)
                        .fieldValue(v)
                        .customFieldDefinition(field)
                        .build());
            }
            return entities;
        }

        public List<ResourceCustomFieldValues> toResourceEntities(CustomFieldDefinitions field, Long resourceId) {
            List<ResourceCustomFieldValues> entities = new ArrayList<>();
            for (String v : values) {
                entities.add(ResourceCustomFieldValues.builder()
                        .resourceId(resourceId)
                        .fieldValue(v)
                        .customFieldDefinition(field)
                        .build());
            }
            return entities;
        }
    }


    @Getter
    @Builder
    @Schema(description = "커스텀 필드 값 조회 응답 DTO")
    public static class CustomFieldValueListRes {

        @Schema(description = "필드 ID", example = "1")
        private Long customFieldId;

        @Schema(description = "필드 이름", example = "회의실 이름")
        private String fieldName;

        @Schema(description = "값 목록", example = "[\"101호 회의실\"]")
        private List<String> values;

        @Schema(description = "데이터 타입", example = "TEXT")
        private String dataType;

        @Schema(description = "필수 여부", example = "true")
        private Boolean required;

        @Schema(description = "설명", example = "회의실 이름을 입력하세요")
        private String description;

        @Schema(description = "선택형 옵션 목록 (RADIO/CHECKBOX)", nullable = true)
        private List<String> options;

        private static String convertBooleanValue(String value) {
            if ("true".equalsIgnoreCase(value)) return "예";
            if ("false".equalsIgnoreCase(value)) return "아니오";
            return value; // boolean이 아니면 원래 값 그대로
        }

        public static CustomFieldValueListRes fromUserEntity(UserCustomFieldValues entity) {
            CustomFieldDefinitions field = entity.getCustomFieldDefinition();

            // RADIO/CHECKBOX 옵션 조회
            List<String> options = null;
            if (field.getCustomFieldSelectDefinitions() != null
                    && !field.getCustomFieldSelectDefinitions().isEmpty()) {
                options = field.getCustomFieldSelectDefinitions().stream()
                        .map(CustomFieldSelectDefinitions::getName)
                        .collect(Collectors.toList());
            }

            return CustomFieldValueListRes.builder()
                    .customFieldId(field.getId())
                    .fieldName(field.getFieldName())
                    .values(List.of(convertBooleanValue(entity.getFieldValue())))
                    .dataType(field.getDataType().name())
                    .required(field.getIsRequired())
                    .description(field.getDescription())
                    .options(options)
                    .build();
        }

        public static CustomFieldValueListRes fromResourceEntity(ResourceCustomFieldValues entity) {
            CustomFieldDefinitions field = entity.getCustomFieldDefinition();

            // RADIO/CHECKBOX 옵션 조회
            List<String> options = null;
            if (field.getCustomFieldSelectDefinitions() != null
                    && !field.getCustomFieldSelectDefinitions().isEmpty()) {
                options = field.getCustomFieldSelectDefinitions().stream()
                        .map(CustomFieldSelectDefinitions::getName)
                        .collect(Collectors.toList());
            }

            return CustomFieldValueListRes.builder()
                    .customFieldId(field.getId())
                    .fieldName(field.getFieldName())
                    .values(List.of(convertBooleanValue(entity.getFieldValue())))
                    .dataType(field.getDataType().name())
                    .required(field.getIsRequired())
                    .description(field.getDescription())
                    .options(options)
                    .build();
        }
    }



    @Getter
    @Builder
    @Schema(description = "RESOURCE 커스텀 필드 값 수정 요청 DTO")
    public static class CustomFieldValueUpdateReq {

        @Schema(description = "수정할 필드의 ID", example = "1")
        private Long customFieldId;

        @Schema(description = "수정할 필드 값의 ID", example = "1")
        private Long customFieldValueId;

        @Schema(description = "수정할 값", example = "회의실 2")
        private String value;
    }
}
