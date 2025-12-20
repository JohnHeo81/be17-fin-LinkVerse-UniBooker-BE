package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.company.model.entity.Companies;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.dto.SuperDto;
import org.example.unibooker.domain.user.model.dto.UserDto;
import org.example.unibooker.domain.user.model.entity.Users;
import org.example.unibooker.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 슈퍼 관리자 서비스
 * - 슈퍼 관리자 로그인만 처리
 * - 기타 관리 기능은 AdminService 활용
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * 슈퍼 관리자 로그인
     * - 이메일로만 조회, SUPER 권한만
     */
    public UserDto.LoginResponseWithToken superLogin(SuperDto.SuperLoginRequest request) {
        return authService.loginWithRole(
                request.getEmail(),
                request.getPassword(),
                UserRole.SUPER
        );
    }

    // ========== 관리자 관리 ==========

    /**
     * 특정 기업의 관리자 목록 조회
     */
    public SuperDto.CompanyManagerListResponse getCompanyManagers(Long companyId) {
        // 기업 존재 확인
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        // 해당 기업의 ADMIN, MANAGER 조회
        List<Users> managers = userRepository.findByCompany_IdAndRoleIn(
                companyId,
                List.of(UserRole.ADMIN, UserRole.MANAGER)
        );

        // DTO 변환
        List<SuperDto.CompanyManagerInfo> managerInfos = managers.stream()
                .map(user -> SuperDto.CompanyManagerInfo.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .toList();

        return SuperDto.CompanyManagerListResponse.builder()
                .managers(managerInfos)
                .build();
    }
}
