package org.example.unibooker.domain.resource.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.example.unibooker.domain.resource.model.entity.CategoryFieldDefinitions;
import org.example.unibooker.domain.resource.model.entity.CustomDataType;
import org.example.unibooker.domain.resource.model.entity.ServiceCategory;

import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "카테고리 별 필수 필드 관련 DTO 클래스들")
public class CategoryFieldDto {

    @Getter
    @Builder
    @Schema(description = "카테고리 필수 입력 필드 생성 & 수정 요청 DTO")
    public static class CategoryFieldReq {

        @NotBlank(message = "필드 이름은 필수입니다.")
        @Size(max = 50, message = "필드 이름은 50자 이하여야 합니다.")
        @Schema(description = "필드 이름", example = "인원수")
        private String fieldName;

        @Size(max = 200, message = "필드 설명은 200자 이하여야 합니다.")
        @Schema(description = "필드 설명", example = "수용 가능한 인원 수를 입력해주세요.")
        private String description;

        @NotNull(message = "데이터 타입은 필수입니다.")
        @Schema(description = "데이터 타입", example = "TEXT, NUMBER 등등")
        private CustomDataType dataType;

        @NotNull(message = "카테고리는 필수입니다.")
        @Schema(description = "필드 적용할 카테고리", example = "RESERVATION")
        private ServiceCategory category;

        public CategoryFieldDefinitions toEntity() {
            return CategoryFieldDefinitions.builder()
                    .fieldName(fieldName)
                    .description(description)
                    .dataType(dataType)
                    .category(category)
                    .build();
        }
    }


    @Getter
    @Builder
    @Schema(description = "카테고리 필드 단일 조회 응답 DTO")
    public static class CategoryFieldDetailRes {

        @Schema(description = "필드 이름", example = "인원수")
        private String fieldName;

        @Schema(description = "필드 설명", example = "수용 가능한 인원 수를 입력해주세요.")
        private String description;

        @Schema(description = "데이터 타입", example = "TEXT, NUMBER 등등")
        private CustomDataType dataType;

        @Schema(description = "필드 적용할 카테고리", example = "RESERVATION")
        private ServiceCategory category;

        public static CategoryFieldDetailRes fromEntity(CategoryFieldDefinitions entity) {
            return CategoryFieldDetailRes.builder()
                    .fieldName(entity.getFieldName())
                    .description(entity.getDescription())
                    .dataType(entity.getDataType())
                    .category(entity.getCategory())
                    .build();
        }
    }


    @Getter
    @Builder
    @Schema(description = "카테고리 필드 목록 조회 응답 DTO")
    public static class CategoryFieldListRes {

        @Schema(description = "카테고리 필드 목록")
        private List<CategoryFieldDetailRes> categoryFields;

        public static CategoryFieldListRes fromEntityList(List<CategoryFieldDefinitions> entities) {
            List<CategoryFieldDetailRes> list = entities.stream()
                    .map(CategoryFieldDetailRes::fromEntity)
                    .collect(Collectors.toList());
            return CategoryFieldListRes.builder()
                    .categoryFields(list)
                    .build();
        }
    }

}
