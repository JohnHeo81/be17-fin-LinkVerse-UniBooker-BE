package org.example.unibooker.infrastructure.email.template;

import org.example.unibooker.domain.user.model.UserRole;

import java.time.LocalDateTime;

/**
 * 이메일 템플릿 렌더링 서비스 인터페이스
 */
public interface EmailTemplateService {

    /**
     * 매니저 계정 생성 이메일 템플릿 렌더링
     *
     * @param name 매니저 이름
     * @param companyName 회사명
     * @param tempPassword 임시 비밀번호
     * @return 렌더링된 HTML 문자열
     */
    String renderManagerCreationTemplate(String name, String companyName, String tempPassword);

    /**
     * 관리자 승인 이메일 템플릿 렌더링
     *
     * @param name 관리자 이름
     * @param companyName 회사명
     * @param tempPassword 임시 비밀번호
     * @param serviceUrl 서비스 URL
     * @return 렌더링된 HTML 문자열
     */
    String renderAdminApprovalTemplate(String name, String companyName, String tempPassword, String serviceUrl);

    /**
     * 비밀번호 찾기 이메일 템플릿 렌더링
     *
     * @param name 사용자 이름
     * @param companyName 회사명
     * @param tempPassword 임시 비밀번호
     * @return 렌더링된 HTML 문자열
     */
    String renderPasswordResetTemplate(String name, String companyName, String tempPassword);

    /**
     * 계정 삭제 완료 이메일 템플릿 렌더링
     *
     * @param name 사용자 이름
     * @param role 사용자 역할 (ADMIN/MANAGER)
     * @return 렌더링된 HTML 문자열
     */
    String renderAccountDeletionTemplate(String name, UserRole role);

    /**
     * 기업 가입 거절 이메일 템플릿 렌더링
     *
     * @param name 관리자 이름
     * @param companyName 기업명
     * @param businessNumber 사업자등록번호
     * @param appliedDate 신청일
     * @param rejectionReason 거절 사유
     * @return 렌더링된 HTML 문자열
     */
    String renderCompanyRejectionTemplate(
            String name,
            String companyName,
            String businessNumber,
            LocalDateTime appliedDate,
            String rejectionReason
    );

    /**
     * 계정 정지 이메일 템플릿 렌더링
     *
     * @param name 사용자 이름
     * @param companyName 회사명
     * @param reason 정지 사유 (nullable)
     * @return 렌더링된 HTML 문자열
     */
    String renderAccountSuspendedTemplate(String name, String companyName, String reason);

    /**
     * 계정 활성화 이메일 템플릿 렌더링
     *
     * @param name 사용자 이름
     * @param companyName 회사명
     * @return 렌더링된 HTML 문자열
     */
    String renderAccountActivatedTemplate(String name, String companyName);

    /**
     * 예약 리마인더 이메일 템플릿 렌더링
     *
     * @param name 사용자 이름
     * @param resourceName 리소스명
     * @param dateTime 예약 일시 (포맷팅된 문자열)
     * @param reminderType 리마인더 타입 (1H / 24H)
     * @return 렌더링된 HTML 문자열
     */
    String renderReservationReminderTemplate(String name, String resourceName, String dateTime, String reminderType);

    /**
     * 예약 취소 안내 이메일 템플릿 렌더링 (관리자에 의한 취소)
     *
     * @param name 사용자 이름
     * @param resourceName 리소스명
     * @param reservationInfo 예약 정보 (포맷팅된 문자열)
     * @return 렌더링된 HTML 문자열
     */
    String renderReservationCancelledByAdminTemplate(String name, String resourceName, String reservationInfo);

    /**
     * 예약 변경 안내 이메일 템플릿 렌더링 (관리자에 의한 변경)
     *
     * @param name 사용자 이름
     * @param resourceName 리소스명
     * @param reservationInfo 예약 정보 (포맷팅된 문자열)
     * @return 렌더링된 HTML 문자열
     */
    String renderReservationModifiedByAdminTemplate(String name, String resourceName, String reservationInfo);
}