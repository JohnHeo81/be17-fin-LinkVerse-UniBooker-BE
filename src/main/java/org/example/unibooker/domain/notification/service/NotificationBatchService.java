package org.example.unibooker.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.notification.model.NotificationStatus;
import org.example.unibooker.domain.notification.model.NotificationType;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 알림 배치 저장 서비스
 * - JdbcTemplate 배치 INSERT로 대량 알림 저장 최적화
 * - 리소스 수정 시 예약자들에게 일괄 알림 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 예약 취소 알림 일괄 저장 (비동기)
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendCancellationNotifications(List<Reservations> reservations) {
        if (reservations.isEmpty()) {
            return;
        }

        log.info("[NotificationBatch] 취소 알림 일괄 저장 시작 - count: {}", reservations.size());

        String sql = """
            INSERT INTO notifications (user_id, category, title, message, status, is_read, retry_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        batchInsert(sql, reservations, NotificationType.RESERVATION_CANCELLED_BY_ADMIN);

        log.info("[NotificationBatch] 취소 알림 일괄 저장 완료 - count: {}", reservations.size());
    }

    /**
     * 예약 수정 알림 일괄 저장 (비동기)
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendModificationNotifications(List<Reservations> reservations) {
        if (reservations.isEmpty()) {
            return;
        }

        log.info("[NotificationBatch] 수정 알림 일괄 저장 시작 - count: {}", reservations.size());

        String sql = """
            INSERT INTO notifications (user_id, category, title, message, status, is_read, retry_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        batchInsert(sql, reservations, NotificationType.RESERVATION_MODIFIED_BY_ADMIN);

        log.info("[NotificationBatch] 수정 알림 일괄 저장 완료 - count: {}", reservations.size());
    }

    /**
     * JdbcTemplate 배치 INSERT 실행
     */
    private void batchInsert(String sql, List<Reservations> reservations, NotificationType type) {
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, reservations, BATCH_SIZE, (PreparedStatement ps, Reservations r) -> {
            String resourceName = r.getResources().getName();
            String title = type.formatTitle(resourceName);
            String message = buildNotificationMessage(r, type);

            ps.setLong(1, r.getUsers().getId());
            ps.setString(2, type.name());
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, NotificationStatus.SENT.name());
            ps.setBoolean(6, false);
            ps.setInt(7, 0);
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
        });
    }

    /**
     * 알림 메시지 생성 (예약 정보 포함)
     */
    private String buildNotificationMessage(Reservations reservation, NotificationType type) {
        StringBuilder sb = new StringBuilder();

        if (type == NotificationType.RESERVATION_CANCELLED_BY_ADMIN) {
            sb.append("관리자에 의해 예약이 취소되었습니다.\n\n");
        } else {
            sb.append("관리자에 의해 예약이 변경되었습니다.\n\n");
        }

        sb.append("■ 예약 정보\n");
        sb.append("- 서비스명: ").append(reservation.getResources().getName()).append("\n");

        if (reservation.getStartDate() != null) {
            sb.append("- 일시: ").append(reservation.getStartDate().format(DATE_FORMATTER));
            if (reservation.getEndDate() != null) {
                sb.append(" ~ ").append(reservation.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            sb.append("\n");
        }

        if (reservation.getRow() != null && reservation.getCol() != null) {
            sb.append("- 좌석: ").append(reservation.getRow()).append("행 ").append(reservation.getCol()).append("열\n");
        }

        if (reservation.getAttendeeCount() != null) {
            sb.append("- 인원: ").append(reservation.getAttendeeCount()).append("명\n");
        }

        sb.append("\n문의사항은 관리자에게 연락 바랍니다.");

        return sb.toString();
    }
}