package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.company.model.CompanyStatus;
import org.example.unibooker.domain.company.model.dto.CompanyDto;
import org.example.unibooker.domain.company.model.entity.Companies;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.notification.model.NotificationType;
import org.example.unibooker.domain.notification.service.NotificationService;
import org.example.unibooker.domain.resource.repository.ResourceGroupRepository;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.UserStatus;
import org.example.unibooker.domain.user.model.dto.AdminDto;
import org.example.unibooker.domain.user.model.dto.SuperDto;
import org.example.unibooker.domain.user.model.dto.UserDto;
import org.example.unibooker.domain.user.model.entity.Users;
import org.example.unibooker.domain.user.repository.UserRepository;
import org.example.unibooker.infrastructure.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 슈퍼 관리자 서비스
 * - 슈퍼 관리자 로그인
 * - 기업 승인/거절 관리
 * - 관리자/매니저 상태 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ResourceGroupRepository resourceGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    // ========== 로그인 ==========

    /**
     * 슈퍼 관리자 로그인
     */
    public UserDto.LoginResponseWithToken superLogin(SuperDto.SuperLoginRequest request) {
        return authService.loginWithRole(
                request.getEmail(),
                request.getPassword(),
                UserRole.SUPER
        );
    }

    // ========== 기업 승인/거절 관리 ==========

    /**
     * 승인 대기 중인 기업 목록 조회
     */
    public List<CompanyDto.PendingResponse> getPendingCompanies() {
        List<Companies> pendingCompanies = companyRepository.findByStatus(CompanyStatus.PENDING);

        return pendingCompanies.stream()
                .map(this::convertToPendingResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기업 상세 정보 조회
     */
    public CompanyDto.DetailResponse getCompanyDetail(Long companyId) {
        // 1. 기업 조회
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        // 2. 관리자 조회
        Users admin = userRepository.findByCompany_IdAndRole(companyId, UserRole.ADMIN)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 3. 플랫폼 이용 현황 데이터 조회
        Long serviceGroupCount = resourceGroupRepository.countByCompanyId(companyId);
        Long userCount = userRepository.countUsersByCompanyId(companyId);
        LocalDateTime lastLoginAt = userRepository.findLastLoginByCompanyId(companyId);

        // 4. DTO 변환 및 반환
        return CompanyDto.DetailResponse.builder()
                .companyId(company.getId())
                .businessNumber(company.getBusinessNumber())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .logoUrl(company.getLogoUrl())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .approvedAt(company.getApprovedAt())
                .approvedBy(company.getApprovedBy())
                .rejectionReason(company.getRejectionReason())
                .adminId(admin.getId())
                .adminName(admin.getName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .userStatus(admin.getStatus())
                .serviceGroupCount(serviceGroupCount != null ? serviceGroupCount : 0L)
                .userCount(userCount != null ? userCount : 0L)
                .lastLoginAt(lastLoginAt)
                .build();
    }

    /**
     * 기업 승인 처리
     */
    @Transactional
    public CompanyDto.ApprovalResponse approveCompany(Long companyId, Long approvedBy) {
        // 1. Company 조회
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        // 2. 중복 승인 방지
        if (company.getStatus() == CompanyStatus.ACTIVE) {
            throw new BaseException(BaseResponseStatus.ALREADY_APPROVED);
        }

        // 3. Admin User 조회
        Users admin = userRepository.findByCompany_IdAndRole(companyId, UserRole.ADMIN)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 4. 새로운 임시 비밀번호 생성
        String newTempPassword = authService.generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(newTempPassword);

        // 5. 비밀번호 및 상태 업데이트
        admin.updatePassword(encodedPassword);
        admin.activate();

        // 6. Company 승인 처리
        company.approve(approvedBy);

        // 7. 명시적 저장
        userRepository.save(admin);
        companyRepository.save(company);

        // 8. 서비스 URL 생성
        String serviceUrl = company.getServiceUrl(baseUrl);

        // 9. 승인 이메일 발송
        try {
            emailService.sendAdminApprovalEmail(
                    admin.getEmail(),
                    admin.getName(),
                    company.getCompanyName(),
                    newTempPassword,
                    serviceUrl
            );
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
        }

        log.info("기업 승인 완료 - companyId: {}, approvedBy: {}", companyId, approvedBy);

        return CompanyDto.ApprovalResponse.builder()
                .message("기업 승인이 완료되었습니다. 승인 이메일이 발송되었습니다.")
                .companyId(company.getId())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .serviceUrl(serviceUrl)
                .status(company.getStatus())
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기업 거절 처리
     */
    @Transactional
    public CompanyDto.ApprovalResponse rejectCompany(Long companyId, String rejectionReason) {
        // 1. Company 조회
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        // 2. PENDING 상태 검증
        if (company.getStatus() != CompanyStatus.PENDING) {
            throw new BaseException(BaseResponseStatus.INVALID_COMPANY_STATUS);
        }

        // 3. Admin 정보 조회
        Users admin = userRepository.findByCompany_IdAndRole(companyId, UserRole.ADMIN)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 4. 거절 이메일 발송
        try {
            emailService.sendCompanyRejectionEmail(
                    admin.getEmail(),
                    admin.getName(),
                    company.getCompanyName(),
                    company.getBusinessNumber(),
                    company.getCreatedAt(),
                    rejectionReason
            );
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
        }

        // 5. Company 상태 변경
        company.reject(rejectionReason);
        companyRepository.save(company);

        // 6. 해당 Company의 모든 Users를 DELETED 상태로 변경
        List<Users> users = userRepository.findByCompany_Id(companyId);
        for (Users user : users) {
            user.delete();
        }
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }

        log.info("기업 거절 완료 - companyId: {}, reason: {}", companyId, rejectionReason);

        return CompanyDto.ApprovalResponse.builder()
                .message("기업 가입 신청이 거절되었습니다. 거절 사유가 이메일로 발송되었습니다.")
                .processedAt(LocalDateTime.now())
                .build();
    }

    // ========== 관리자/매니저 관리 ==========

    /**
     * 특정 기업의 관리자 목록 조회
     */
    public SuperDto.CompanyManagerListResponse getCompanyManagers(Long companyId) {
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        List<Users> managers = userRepository.findByCompany_IdAndRoleIn(
                companyId,
                List.of(UserRole.ADMIN, UserRole.MANAGER)
        );

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

    /**
     * 전체 관리자/매니저 목록 조회
     */
    public AdminDto.AdminListResponse getAllAdmins(int page, int size, UserRole role, UserStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Users> adminPage;

        if (role != null && status != null) {
            adminPage = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            adminPage = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            adminPage = userRepository.findByStatus(status, pageable);
        } else {
            adminPage = userRepository.findByRoleIn(
                    List.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.SUPER),
                    pageable
            );
        }

        List<AdminDto.AdminListResponse.AdminInfo> admins = adminPage.getContent().stream()
                .map(this::convertToAdminInfo)
                .collect(Collectors.toList());

        return AdminDto.AdminListResponse.builder()
                .admins(admins)
                .totalElements(adminPage.getTotalElements())
                .totalPages(adminPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * 관리자/매니저 상태 변경 (알림 발송 포함)
     */
    @Transactional
    public void updateAdminStatus(Long userId, AdminDto.AdminStatusUpdateRequest request) {
        // 1. 사용자 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 권한 확인 (ADMIN 또는 MANAGER만)
        if (!user.hasAdminAuthority() && !user.isManager()) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        // 3. 상태 변경
        switch (request.getStatus()) {
            case ACTIVE:
                user.activate();
                break;
            case INACTIVE:
                user.deactivate();
                break;
            case SUSPENDED:
                user.suspend();
                break;
            case DELETED:
                user.delete();
                break;
            default:
                throw new BaseException(BaseResponseStatus.INVALID_USER_STATUS);
        }

        userRepository.save(user);

        // 4. 알림 발송
        if (request.getStatus() == UserStatus.ACTIVE) {
            notificationService.sendNotificationToUser(NotificationType.ACCOUNT_ACTIVATED, user);
        } else if (request.getStatus() == UserStatus.SUSPENDED) {
            notificationService.sendNotificationToUser(NotificationType.ACCOUNT_SUSPENDED, user);
        }

        log.info("관리자 상태 변경 - userId: {}, newStatus: {}", userId, request.getStatus());
    }

    // ========== 변환 메서드 ==========

    /**
     * Company -> PendingResponse DTO 변환
     */
    private CompanyDto.PendingResponse convertToPendingResponse(Companies company) {
        Users admin = userRepository.findByCompany_IdAndRole(company.getId(), UserRole.ADMIN)
                .orElse(null);

        return CompanyDto.PendingResponse.builder()
                .companyId(company.getId())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .logoUrl(company.getLogoUrl())
                .adminName(admin != null ? admin.getName() : null)
                .email(admin != null ? admin.getEmail() : null)
                .phone(admin != null ? admin.getPhone() : null)
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .build();
    }

    /**
     * User -> AdminInfo DTO 변환
     */
    private AdminDto.AdminListResponse.AdminInfo convertToAdminInfo(Users admin) {
        Companies company = null;
        if (admin.getCompany() != null && admin.getCompany().getId() != null) {
            company = companyRepository.findById(admin.getCompany().getId()).orElse(null);
        }

        return AdminDto.AdminListResponse.AdminInfo.builder()
                .userId(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .role(admin.getRole())
                .status(admin.getStatus())
                .companyId(admin.getCompany() != null ? admin.getCompany().getId() : null)
                .companyName(company != null ? company.getCompanyName() : null)
                .companySlug(company != null ? company.getCompanySlug() : null)
                .createdAt(admin.getCreatedAt())
                .lastLoginAt(null)
                .build();
    }
}