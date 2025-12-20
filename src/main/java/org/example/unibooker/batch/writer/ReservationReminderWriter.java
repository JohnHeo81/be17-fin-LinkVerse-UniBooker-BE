package org.example.unibooker.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.batch.reader.ReservationReminderReader.ReminderItem;
import org.example.unibooker.domain.notification.model.NotificationType;
import org.example.unibooker.domain.notification.model.ReminderType;
import org.example.unibooker.domain.notification.model.entity.ReminderLog;
import org.example.unibooker.domain.notification.repository.ReminderLogRepository;
import org.example.unibooker.domain.notification.service.NotificationService;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 리마인더 Writer
 * - 알림 발송
 * - 발송 로그 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderWriter implements ItemWriter<ReminderItem> {

    private final NotificationService notificationService;
    private final ReminderLogRepository reminderLogRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");

    @Override
    public void write(Chunk<? extends ReminderItem> chunk) {
        for (ReminderItem item : chunk) {
            Reservations reservation = item.reservation();
            ReminderType type = item.type();

            try {
                // 알림 타입 결정
                NotificationType notificationType = (type == ReminderType.HOUR_1)
                        ? NotificationType.RESERVATION_REMINDER_1H
                        : NotificationType.RESERVATION_REMINDER_24H;

                // 알림 발송
                String timeInfo = reservation.getStartDate().format(DATE_FORMAT);
                notificationService.sendNotificationToUser(
                        notificationType,
                        reservation.getUsers(),
                        reservation.getResources().getName(),
                        timeInfo
                );

                // 발송 로그 저장
                reminderLogRepository.save(ReminderLog.of(reservation.getId(), type));

                log.info("✅ [리마인더 발송] reservationId={}, type={}, user={}",
                        reservation.getId(), type, reservation.getUsers().getEmail());

            } catch (Exception e) {
                log.error("❌ [리마인더 실패] reservationId={}, type={}, error={}",
                        reservation.getId(), type, e.getMessage());
            }
        }
    }
}