package org.example.unibooker.domain.company.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponse;
import org.example.unibooker.domain.company.model.dto.CompanyDto;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.company.service.CompanyService;
import org.springframework.web.bind.annotation.*;

/**
 * 기업 공개 API
 * - Slug/사업자번호 중복 확인 (회원가입용)
 * - Slug로 기업 공개 정보 조회 (일반 사용자용)
 */
@Slf4j
@Tag(name = "Company API", description = "기업 공개 API")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final CompanyService companyService;

    @Operation(summary = "Company Slug 중복 확인",
            description = "회원가입 시 사용할 Company Slug의 사용 가능 여부를 확인합니다.")
    @GetMapping("/check-slug")
    public BaseResponse<CompanyDto.SlugCheckResponse> checkSlug(
            @RequestParam String slug) {

        CompanyDto.SlugCheckResponse response = companyService.checkSlugAvailability(slug);
        return BaseResponse.success(response);
    }

    @Operation(summary = "사업자등록번호 중복 확인",
            description = "사업자등록번호가 이미 등록되어 있는지 확인합니다.")
    @GetMapping("/check-business-number")
    public BaseResponse<Boolean> checkBusinessNumber(
            @RequestParam @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$",
                    message = "올바른 사업자등록번호 형식이 아닙니다")
            String businessNumber) {

        boolean exists = companyRepository.existsByBusinessNumber(businessNumber);
        return BaseResponse.success(exists);
    }

    /**
     * Company Slug로 기업 정보 조회 (일반 사용자용)
     */
    @Operation(summary = "Company Slug로 기업 정보 조회",
            description = "Company Slug를 통해 기업 정보를 조회합니다. (일반 사용자 회원가입용)")
    @GetMapping("/slug/{companySlug}")
    public BaseResponse<CompanyDto.PublicInfoResponse> getCompanyBySlug(
            @PathVariable @Schema(description = "Company Slug", example = "company-a") String companySlug) {

        CompanyDto.PublicInfoResponse response = companyService.getCompanyBySlug(companySlug);
        return BaseResponse.success(response);
    }
}