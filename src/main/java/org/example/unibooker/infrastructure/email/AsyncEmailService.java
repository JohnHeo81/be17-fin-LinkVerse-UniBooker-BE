package org.example.unibooker.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 비동기 이메일 발송 서비스
 * - 리소스 수정 시 예약자들에게 일괄 이메일 발송
 * - @Async로 백그라운드 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEmailService {

    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 예약 취소 이메일 일괄 발송 (비동기)
     */
    @Async("emailExecutor")
    public void sendCancellationEmails(List<Reservations> reservations) {
        if (reservations.isEmpty()) {
            return;
        }

        log.info("[AsyncEmail] 취소 이메일 일괄 발송 시작 - count: {}", reservations.size());

        int successCount = 0;
        int failCount = 0;

        for (Reservations reservation : reservations) {
            try {
                String to = reservation.getUsers().getEmail();
                String name = reservation.getUsers().getName();
                String resourceName = reservation.getResources().getName();
                String reservationInfo = buildReservationInfo(reservation);

                emailService.sendReservationCancelledByAdminEmail(to, name, resourceName, reservationInfo);
                successCount++;

            } catch (Exception e) {
                failCount++;
                log.error("[AsyncEmail] 취소 이메일 발송 실패 - reservationId: {}, error: {}",
                        reservation.getId(), e.getMessage());
            }
        }

        log.info("[AsyncEmail] 취소 이메일 일괄 발송 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 예약 수정 이메일 일괄 발송 (비동기)
     */
    @Async("emailExecutor")
    public void sendModificationEmails(List<Reservations> reservations) {
        if (reservations.isEmpty()) {
            return;
        }

        log.info("[AsyncEmail] 수정 이메일 일괄 발송 시작 - count: {}", reservations.size());

        int successCount = 0;
        int failCount = 0;

        for (Reservations reservation : reservations) {
            try {
                String to = reservation.getUsers().getEmail();
                String name = reservation.getUsers().getName();
                String resourceName = reservation.getResources().getName();
                String reservationInfo = buildReservationInfo(reservation);

                emailService.sendReservationModifiedByAdminEmail(to, name, resourceName, reservationInfo);
                successCount++;

            } catch (Exception e) {
                failCount++;
                log.error("[AsyncEmail] 수정 이메일 발송 실패 - reservationId: {}, error: {}",
                        reservation.getId(), e.getMessage());
            }
        }

        log.info("[AsyncEmail] 수정 이메일 일괄 발송 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 예약 정보 문자열 생성
     */
    private String buildReservationInfo(Reservations reservation) {
        StringBuilder sb = new StringBuilder();

        sb.append("서비스명: ").append(reservation.getResources().getName()).append("\n");

        if (reservation.getStartDate() != null) {
            sb.append("일시: ").append(reservation.getStartDate().format(DATE_FORMATTER));
            if (reservation.getEndDate() != null) {
                sb.append(" ~ ").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            sb.append("\n");
        }

        if (reservation.getRow() != null && reservation.getCol() != null) {
            sb.append("좌석: ").append(reservation.getRow()).append("행 ").append(reservation.getCol()).append("열\n");
        }

        if (reservation.getAttendeeCount() != null) {
            sb.append("인원: ").append(reservation.getAttendeeCount()).append("명\n");
        }

        return sb.toString();
    }
}