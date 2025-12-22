package org.example.unibooker.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.common.BaseResponseStatus;
import org.example.unibooker.common.exception.BaseException;
import org.example.unibooker.domain.company.constants.ReservedSlugs;
import org.example.unibooker.domain.company.model.CompanyStatus;
import org.example.unibooker.domain.company.model.entity.Companies;
import org.example.unibooker.domain.company.repository.CompanyRepository;
import org.example.unibooker.domain.notification.model.NotificationType;
import org.example.unibooker.domain.notification.service.NotificationService;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.domain.user.model.UserStatus;
import org.example.unibooker.domain.user.model.dto.AdminDto;
import org.example.unibooker.domain.user.model.dto.ManagerDto;
import org.example.unibooker.domain.user.model.dto.UserDto;
import org.example.unibooker.domain.user.model.entity.Users;
import org.example.unibooker.domain.user.repository.UserRepository;
import org.example.unibooker.infrastructure.email.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 관리자(Admin/Manager) 서비스
 * - 관리자 회원가입 및 로그인
 * - 비밀번호 재설정
 * - 매니저 CRUD
 * - 기업 로고 변경
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /** Company Slug 유효성 검증 패턴 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{3,30}$");

    /** 승인 예상 소요 일수 */
    private static final int ESTIMATED_APPROVAL_DAYS = 3;

    // ========== 로그인 ==========

    /**
     * 관리자/매니저 로그인
     */
    public UserDto.LoginResponseWithToken adminLogin(AdminDto.AdminLoginRequest request) {
        return authService.loginWithRoles(
                request.getEmail(),
                request.getPassword(),
                List.of(UserRole.ADMIN, UserRole.MANAGER)
        );
    }

    // ========== 회원가입 ==========

    /**
     * 관리자 회원가입 처리
     */
    @Transactional
    public AdminDto.SignUpResponse signUpAdmin(AdminDto.SignUpRequest request) {
        // 1. 사업자등록번호, Slug 중복 검증
        validateDuplicateBusinessNumber(request.getBusinessNumber());
        validateCompanySlug(request.getCompanySlug());

        // 2. 탈퇴한 ADMIN 계정이 있는지 확인
        Optional<Users> deletedAdmin = userRepository.findByEmailAndStatus(request.getEmail(), UserStatus.DELETED)
                .filter(Users::isAdmin);

        Companies company;
        Users admin;

        if (deletedAdmin.isPresent()) {
            // 탈퇴 ADMIN 계정 복구
            admin = deletedAdmin.get();
            admin.restore();

            company = createCompany(request);
            company = companyRepository.save(company);

            String temporaryPassword = authService.generateTemporaryPassword();
            String encodedPassword = passwordEncoder.encode(temporaryPassword);

            admin.updatePassword(encodedPassword);
            admin.updateName(request.getName());
            admin.updatePhone(request.getPhone());
            admin.updateCompany(company);
            admin.deactivate();
        } else {
            // DELETED 아닌 상태에서 이메일 중복 확인
            validateDuplicateEmail(request.getEmail());

            // 신규 Company 및 Admin 생성
            company = createCompany(request);
            company = companyRepository.save(company);

            String temporaryPassword = authService.generateTemporaryPassword();
            String encodedPassword = passwordEncoder.encode(temporaryPassword);

            admin = createAdmin(request, company, encodedPassword);
        }

        userRepository.save(admin);

        // SUPER 관리자에게 신규 신청 알림 저장
        notificationService.saveNotificationToRole(NotificationType.NEW_COMPANY_REQUEST, UserRole.SUPER);

        return AdminDto.SignUpResponse.builder()
                .message("관리자 회원가입 신청이 완료되었습니다. 승인까지 최대 " + ESTIMATED_APPROVAL_DAYS + "일이 소요될 수 있습니다.")
                .email(request.getEmail())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .serviceUrl(null)
                .estimatedDays(ESTIMATED_APPROVAL_DAYS)
                .build();
    }

    /**
     * 회원가입 신청 상태 조회
     */
    public AdminDto.StatusResponse checkSignUpStatus(String email) {
        List<Users> users = userRepository.findByEmailAndRoleIn(
                email,
                List.of(UserRole.ADMIN, UserRole.MANAGER)
        );

        if (users.isEmpty()) {
            throw new BaseException(BaseResponseStatus.USER_NOT_FOUND);
        }

        Users user = users.get(0);

        Companies company = companyRepository.findById(user.getCompany().getId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        return AdminDto.StatusResponse.builder()
                .status(company.getStatus())
                .companyName(company.getCompanyName())
                .companySlug(company.getCompanySlug())
                .email(user.getEmail())
                .rejectionReason(company.getRejectionReason())
                .appliedAt(company.getCreatedAt())
                .build();
    }

    // ========== 비밀번호 재설정 ==========

    /**
     * 비밀번호 재설정 (첫 로그인 시)
     */
    @Transactional
    public AdminDto.PasswordResetResponse resetPassword(Long userId, AdminDto.PasswordResetRequest request) {
        // 1. 사용자 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BaseException(BaseResponseStatus.CURRENT_PASSWORD_INCORRECT);
        }

        // 3. 새 비밀번호와 확인 비밀번호 일치 여부
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(BaseResponseStatus.PASSWORD_MISMATCH);
        }

        // 4. 새 비밀번호가 현재 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BaseException(BaseResponseStatus.SAME_PASSWORD);
        }

        // 5. 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.updatePassword(encodedPassword);

        if (user.getIsFirstLogin()) {
            user.completeFirstLogin();
        }

        // 6. 모든 Refresh Token 삭제
        authService.invalidateAllTokens(userId);

        userRepository.save(user);

        return AdminDto.PasswordResetResponse.builder()
                .message("비밀번호가 성공적으로 변경되었습니다.")
                .passwordChangeRequired(false)
                .build();
    }

    // ========== 기업 로고 ==========

    /**
     * 기업 로고 업데이트
     */
    @Transactional
    public void updateCompanyLogo(Long userId, String logoUrl) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        if (!user.hasAdminAuthority() && !user.isManager()) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        Companies company = user.getCompany();
        if (company == null) {
            throw new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND);
        }

        company.updateLogoUrl(logoUrl);
    }

    // ========== 매니저 관리 ==========

    /**
     * 매니저 계정 생성
     */
    @Transactional
    public ManagerDto.CreateResponse createManager(ManagerDto.CreateRequest request, Long currentUserId) {
        // 1. Admin 권한 검증
        Users admin = validateAdminAuthority(currentUserId);

        // 2. Company 승인 상태 검증
        Companies company = validateCompanyStatus(admin.getCompany().getId());

        // 3. 이메일 중복 검증
        validateManagerEmailDuplicate(request.getEmail());

        // 4. 같은 기업의 DELETED MANAGER 계정 찾기
        Optional<Users> deletedManager = userRepository.findByEmailAndCompany_IdAndRoleAndStatus(
                request.getEmail(),
                admin.getCompany().getId(),
                UserRole.MANAGER,
                UserStatus.DELETED
        );

        Users manager;
        String temporaryPassword = authService.generateTemporaryPassword();

        if (deletedManager.isPresent()) {
            // DELETED MANAGER 재활용
            manager = deletedManager.get();
            manager.restore();

            String encodedPassword = passwordEncoder.encode(temporaryPassword);
            manager.updatePassword(encodedPassword);
            manager.updateName(request.getName());
            manager.updatePhone(request.getPhone());

            userRepository.save(manager);
        } else {
            // 신규 MANAGER 생성
            String encodedPassword = passwordEncoder.encode(temporaryPassword);

            manager = Users.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .name(request.getName())
                    .phone(request.getPhone())
                    .role(UserRole.MANAGER)
                    .status(UserStatus.ACTIVE)
                    .company(company)
                    .isFirstLogin(true)
                    .build();

            userRepository.save(manager);
        }

        // 5. 이메일 발송
        try {
            emailService.sendManagerCreationEmail(
                    request.getEmail(),
                    request.getName(),
                    company.getCompanyName(),
                    temporaryPassword
            );
        } catch (Exception e) {
            throw new BaseException(BaseResponseStatus.EMAIL_SEND_FAILED);
        }

        return ManagerDto.CreateResponse.builder()
                .message("매니저 계정이 성공적으로 생성되었습니다. 이메일을 확인해주세요.")
                .managerId(manager.getId())
                .email(manager.getEmail())
                .name(manager.getName())
                .companyName(company.getCompanyName())
                .createdAt(manager.getCreatedAt())
                .build();
    }

    /**
     * 매니저 목록 조회
     */
    public ManagerDto.ManagerListResponse getManagers(Long adminUserId, int page, int size) {
        Users admin = validateAdminAuthority(adminUserId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Users> managerPage = userRepository.findByCompany_IdAndRoleAndStatusNot(
                admin.getCompany().getId(),
                UserRole.MANAGER,
                UserStatus.DELETED,
                pageable
        );

        List<ManagerDto.ManagerListResponse.ManagerInfo> managers = managerPage.getContent().stream()
                .map(this::convertToManagerInfo)
                .collect(Collectors.toList());

        return ManagerDto.ManagerListResponse.builder()
                .managers(managers)
                .totalElements(managerPage.getTotalElements())
                .totalPages(managerPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * 매니저 정보 수정
     */
    @Transactional
    public ManagerDto.UpdateResponse updateManager(Long managerId, ManagerDto.UpdateRequest request, Long adminUserId) {
        Users admin = validateAdminAuthority(adminUserId);

        Users manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        if (!manager.isManager()) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        if (!manager.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        manager.updateName(request.getName());
        manager.updatePhone(request.getPhone());
        userRepository.save(manager);

        return ManagerDto.UpdateResponse.builder()
                .message("매니저 정보가 성공적으로 수정되었습니다.")
                .managerId(manager.getId())
                .name(manager.getName())
                .email(manager.getEmail())
                .phone(manager.getPhone())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 매니저 계정 삭제
     */
    @Transactional
    public ManagerDto.ManagerDeleteResponse deleteManager(Long managerId, Long adminUserId) {
        Users admin = validateAdminAuthority(adminUserId);

        Users manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        if (!manager.isManager()) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        if (!manager.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        manager.delete();
        userRepository.save(manager);

        return ManagerDto.ManagerDeleteResponse.builder()
                .message("매니저 계정이 삭제되었습니다.")
                .managerId(manager.getId())
                .name(manager.getName())
                .email(manager.getEmail())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    // ========== 검증 메서드 ==========

    /** Admin 권한 검증 */
    private Users validateAdminAuthority(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        if (!user.hasAdminAuthority()) {
            throw new BaseException(BaseResponseStatus.UNAUTHORIZED_ACTION);
        }

        return user;
    }

    /** Company 승인 상태 검증 */
    private Companies validateCompanyStatus(Long companyId) {
        Companies company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COMPANY_NOT_FOUND));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BaseException(BaseResponseStatus.COMPANY_NOT_APPROVED);
        }

        return company;
    }

    /** Company Slug 유효성 검증 */
    private void validateCompanySlug(String slug) {
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new BaseException(BaseResponseStatus.INVALID_SLUG_FORMAT);
        }

        if (slug.startsWith("-") || slug.endsWith("-")) {
            throw new BaseException(BaseResponseStatus.INVALID_SLUG_FORMAT);
        }

        if (slug.contains("--")) {
            throw new BaseException(BaseResponseStatus.INVALID_SLUG_FORMAT);
        }

        if (ReservedSlugs.isReserved(slug)) {
            throw new BaseException(BaseResponseStatus.RESERVED_SLUG);
        }

        if (companyRepository.existsByCompanySlug(slug)) {
            throw new BaseException(BaseResponseStatus.DUPLICATE_SLUG);
        }
    }

    /** ADMIN/MANAGER 이메일 중복 검증 */
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmailAndRoleInAndStatusNot(
                email,
                List.of(UserRole.ADMIN, UserRole.MANAGER),
                UserStatus.DELETED)) {
            throw new BaseException(BaseResponseStatus.DUPLICATE_EMAIL);
        }
    }

    /** 사업자등록번호 중복 검증 */
    private void validateDuplicateBusinessNumber(String businessNumber) {
        if (companyRepository.existsByBusinessNumber(businessNumber)) {
            throw new BaseException(BaseResponseStatus.DUPLICATE_BUSINESS_NUMBER);
        }
    }

    /** MANAGER 이메일 중복 검증 */
    private void validateManagerEmailDuplicate(String email) {
        if (userRepository.existsByEmailAndRoleInAndStatusNot(
                email,
                List.of(UserRole.ADMIN, UserRole.MANAGER),
                UserStatus.DELETED)) {
            throw new BaseException(BaseResponseStatus.ADMIN_MANAGER_EMAIL_EXISTS);
        }
    }

    // ========== 엔티티 생성 ==========

    /** Company 엔티티 생성 */
    private Companies createCompany(AdminDto.SignUpRequest request) {
        return Companies.builder()
                .businessNumber(request.getBusinessNumber())
                .companyName(request.getCompanyName())
                .companySlug(request.getCompanySlug())
                .logoUrl(request.getLogoUrl())
                .status(CompanyStatus.PENDING)
                .build();
    }

    /** Admin User 엔티티 생성 */
    private Users createAdmin(AdminDto.SignUpRequest request, Companies company, String encodedPassword) {
        return Users.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .role(UserRole.ADMIN)
                .status(UserStatus.INACTIVE)
                .company(company)
                .isFirstLogin(true)
                .build();
    }

    // ========== DTO 변환 ==========

    /** User -> ManagerInfo DTO 변환 */
    private ManagerDto.ManagerListResponse.ManagerInfo convertToManagerInfo(Users manager) {
        return ManagerDto.ManagerListResponse.ManagerInfo.builder()
                .managerId(manager.getId())
                .name(manager.getName())
                .email(manager.getEmail())
                .phone(manager.getPhone())
                .status(manager.getStatus())
                .isFirstLogin(manager.getIsFirstLogin())
                .createdAt(manager.getCreatedAt())
                .lastLoginAt(null)
                .build();
    }
}