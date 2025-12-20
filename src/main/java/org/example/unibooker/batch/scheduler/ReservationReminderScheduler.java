package org.example.unibooker.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.notification.repository.ReminderLogRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 예약 리마인더 스케줄러
 * - 매 정각 실행
 * - 1시간 전, 24시간 전 알림 발송
 * - 지난 예약 로그 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderScheduler {

    private final JobLauncher jobLauncher;
    private final Job reservationReminderJob;
    private final ReminderLogRepository reminderLogRepository;

    /** 매 정각 실행 */
    @Scheduled(cron = "0 0 * * * *")
    public void runReminderJob() {
        try {
            log.info("🔔 [리마인더 배치 시작]");

            // 1. 리마인더 발송 Job 실행
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(reservationReminderJob, jobParameters);

            // 2. 지난 예약 로그 정리
            cleanupOldLogs();

            log.info("✅ [리마인더 배치 완료]");

        } catch (Exception e) {
            log.error("❌ [리마인더 배치 실패] {}", e.getMessage(), e);
        }
    }

    /** 지난 예약 로그 삭제 */
    @Transactional
    public void cleanupOldLogs() {
        int deleted = reminderLogRepository.deleteByReservationStartDateBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("🗑️ [로그 정리] {}건 삭제", deleted);
        }
    }
}