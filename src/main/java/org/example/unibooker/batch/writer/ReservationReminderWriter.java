package org.example.unibooker.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.batch.reader.ReservationReminderReader.ReminderItem;
import org.example.unibooker.domain.notification.model.ReminderType;
import org.example.unibooker.domain.notification.model.entity.ReminderLog;
import org.example.unibooker.domain.notification.repository.ReminderLogRepository;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.example.unibooker.infrastructure.email.EmailService;
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

    private final ReminderLogRepository reminderLogRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");

    @Override
    public void write(Chunk<? extends ReminderItem> chunk) {
        for (ReminderItem item : chunk) {
            Reservations reservation = item.reservation();
            ReminderType type = item.type();

            try {
                String timeInfo = reservation.getStartDate().format(DATE_FORMAT);
                String reminderType = (type == ReminderType.HOUR_1) ? "1H" : "24H";

                // 이메일 발송
                emailService.sendReservationReminderEmail(
                        reservation.getUsers().getEmail(),
                        reservation.getUsers().getName(),
                        reservation.getResources().getName(),
                        timeInfo,
                        reminderType
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