package org.example.unibooker.domain.notification.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.unibooker.domain.notification.model.ReminderType;

import java.time.LocalDateTime;

/**
 * 리마인더 발송 로그
 * - 중복 발송 방지용
 * - 예약 시간 지나면 삭제
 */
@Entity
@Table(name = "reminder_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reservation_id", "type"})
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예약 ID */
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    /** 리마인더 타입 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType type;

    /** 발송 시각 */
    @Column(nullable = false)
    private LocalDateTime sentAt;

    public static ReminderLog of(Long reservationId, ReminderType type) {
        return ReminderLog.builder()
                .reservationId(reservationId)
                .type(type)
                .sentAt(LocalDateTime.now())
                .build();
    }
}