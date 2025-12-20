package org.example.unibooker.domain.notification.repository;

import org.example.unibooker.domain.notification.model.ReminderType;
import org.example.unibooker.domain.notification.model.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 리마인더 로그 레포지토리
 */
@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    /** 중복 체크 */
    boolean existsByReservationIdAndType(Long reservationId, ReminderType type);

    /** 지난 예약의 로그 삭제 */
    @Modifying
    @Query("""
        DELETE FROM ReminderLog rl
        WHERE rl.reservationId IN (
            SELECT r.id FROM Reservations r WHERE r.startDate < :now
        )
    """)
    int deleteByReservationStartDateBefore(LocalDateTime now);
}