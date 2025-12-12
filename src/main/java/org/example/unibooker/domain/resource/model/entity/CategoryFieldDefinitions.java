package org.example.unibooker.domain.resource.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.unibooker.common.BaseEntity;
import org.example.unibooker.domain.resource.model.dto.CategoryFieldDto;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class CategoryFieldDefinitions extends BaseEntity {
    private String fieldName;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    public void update(CategoryFieldDto.CategoryFieldReq dto) {
        this.fieldName = dto.getFieldName();
        this.description = dto.getDescription();
        this.dataType = dto.getDataType();
        this.category = dto.getCategory();
    }
}
