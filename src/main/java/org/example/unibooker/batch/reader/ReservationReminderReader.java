package org.example.unibooker.batch.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.notification.model.ReminderType;
import org.example.unibooker.domain.notification.repository.ReminderLogRepository;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.example.unibooker.domain.reservation.repository.ReservationRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * 리마인더 대상 예약 Reader
 * - 1시간 전, 24시간 전 예약 조회
 * - 이미 발송된 예약 제외
 */
@Slf4j
@Component
public class ReservationReminderReader implements ItemReader<ReservationReminderReader.ReminderItem> {

    private final ReservationRepository reservationRepository;
    private final ReminderLogRepository reminderLogRepository;

    private Iterator<ReminderItem> itemIterator;
    private boolean initialized = false;

    public ReservationReminderReader(ReservationRepository reservationRepository,
                                     ReminderLogRepository reminderLogRepository) {
        this.reservationRepository = reservationRepository;
        this.reminderLogRepository = reminderLogRepository;
    }

    @Override
    public ReminderItem read() {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (itemIterator != null && itemIterator.hasNext()) {
            return itemIterator.next();
        }

        // 다음 Job 실행을 위해 초기화
        initialized = false;
        itemIterator = null;
        return null;
    }

    private void initialize() {
        LocalDateTime now = LocalDateTime.now();

        // 1시간 전 리마인더 대상 (1시간 ~ 2시간 후 예약)
        List<Reservations> targets1h = reservationRepository
                .findReminderTargets(now.plusHours(1), now.plusHours(2));

        // 24시간 전 리마인더 대상 (24시간 ~ 25시간 후 예약)
        List<Reservations> targets24h = reservationRepository
                .findReminderTargets(now.plusHours(24), now.plusHours(25));

        // ReminderItem으로 변환 (미발송 건만)
        List<ReminderItem> items = new java.util.ArrayList<>();

        for (Reservations r : targets1h) {
            if (!reminderLogRepository.existsByReservationIdAndType(r.getId(), ReminderType.HOUR_1)) {
                items.add(new ReminderItem(r, ReminderType.HOUR_1));
            }
        }

        for (Reservations r : targets24h) {
            if (!reminderLogRepository.existsByReservationIdAndType(r.getId(), ReminderType.HOUR_24)) {
                items.add(new ReminderItem(r, ReminderType.HOUR_24));
            }
        }

        this.itemIterator = items.iterator();
        log.info("📋 [리마인더 Reader] 대상 건수: 1시간 전={}, 24시간 전={}, 총={}",
                targets1h.size(), targets24h.size(), items.size());
    }

    /**
     * 예약 + 리마인더 타입 묶음
     */
    public record ReminderItem(Reservations reservation, ReminderType type) {}
}