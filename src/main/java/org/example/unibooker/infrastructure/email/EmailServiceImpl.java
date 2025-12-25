package org.example.unibooker.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.user.model.UserRole;
import org.example.unibooker.infrastructure.email.template.EmailTemplateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 이메일 발송 서비스 구현체
 * JavaMailSender를 사용하여 실제 이메일을 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML 형식

            mailSender.send(message);
            log.info("✅ 이메일 발송 성공 - 수신자: {}, 제목: {}", to, subject);

        } catch (MessagingException e) {
            log.error("❌ 이메일 메시지 생성 실패 - 수신자: {}", to, e);
            throw new RuntimeException("이메일 메시지 생성에 실패했습니다", e);
        } catch (MailException e) {
            log.error("❌ 이메일 발송 실패 - 수신자: {}", to, e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    @Override
    public void sendManagerCreationEmail(String to, String name, String companyName, String tempPassword) {
        String htmlContent = emailTemplateService.renderManagerCreationTemplate(name, companyName, tempPassword);
        String subject = "[UniBooker] 매니저 계정이 생성되었습니다";

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendAdminApprovalEmail(String to, String name, String companyName, String tempPassword, String serviceUrl) {
        String htmlContent = emailTemplateService.renderAdminApprovalTemplate(name, companyName, tempPassword, serviceUrl);
        String subject = "[UniBooker] 기업 가입이 승인되었습니다";

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendPasswordResetEmail(String to, String name, String companyName, String tempPassword) {
        String htmlContent = emailTemplateService.renderPasswordResetTemplate(name, companyName, tempPassword);
        String subject = "[UniBooker] 임시 비밀번호가 발급되었습니다";

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendAccountDeletionNotice(String to, String name, UserRole role) {
        String htmlContent = emailTemplateService.renderAccountDeletionTemplate(name, role);
        String subject = "[UniBooker] 계정이 삭제되었습니다";

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendCompanyRejectionEmail(
            String to,
            String name,
            String companyName,
            String businessNumber,
            LocalDateTime appliedDate,
            String rejectionReason) {

        String htmlContent = emailTemplateService.renderCompanyRejectionTemplate(
                name,
                companyName,
                businessNumber,
                appliedDate,
                rejectionReason
        );
        String subject = "[UniBooker] 기업 가입 신청이 거절되었습니다";

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendAccountSuspendedEmail(String to, String name, String companyName, String reason) {
        String subject = "[UniBooker] 계정 정지 안내";
        String htmlContent = emailTemplateService.renderAccountSuspendedTemplate(name, companyName, reason);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendAccountActivatedEmail(String to, String name, String companyName) {
        String subject = "[UniBooker] 계정 활성화 안내";
        String htmlContent = emailTemplateService.renderAccountActivatedTemplate(name, companyName);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendReservationReminderEmail(String to, String name, String resourceName, String dateTime, String reminderType) {
        String subject = reminderType.equals("1H")
                ? "[UniBooker] 예약 1시간 전 알림"
                : "[UniBooker] 예약 24시간 전 알림";
        String htmlContent = emailTemplateService.renderReservationReminderTemplate(name, resourceName, dateTime, reminderType);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendReservationCancelledByAdminEmail(String to, String name, String resourceName, String reservationInfo) {
        String subject = "[UniBooker] 예약 취소 안내";
        String htmlContent = emailTemplateService.renderReservationCancelledByAdminTemplate(name, resourceName, reservationInfo);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendReservationModifiedByAdminEmail(String to, String name, String resourceName, String reservationInfo) {
        String subject = "[UniBooker] 예약 변경 안내";
        String htmlContent = emailTemplateService.renderReservationModifiedByAdminTemplate(name, resourceName, reservationInfo);
        sendHtmlEmail(to, subject, htmlContent);
    }
}