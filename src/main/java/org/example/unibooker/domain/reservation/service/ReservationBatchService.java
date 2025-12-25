package org.example.unibooker.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.reservation.model.entity.Reservations;
import org.example.unibooker.domain.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 일괄 처리 서비스
 * - 리소스 수정 시 영향받는 예약 일괄 취소/수정
 * - 벌크 UPDATE로 대량 처리 최적화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationBatchService {

    private final ReservationRepository reservationRepository;

    /**
     * 리소스의 확정된 예약 목록 조회
     * - 알림/메일 발송 전 예약자 정보 조회용
     */
    @Transactional(readOnly = true)
    public List<Reservations> getConfirmedReservations(Long resourceId) {
        return reservationRepository.findConfirmedByResourceId(resourceId);
    }

    /**
     * 리소스의 예약 일괄 취소
     * - 상태를 CANCELLED로 변경
     * - deletedAt 설정 (소프트 삭제)
     */
    @Transactional
    public int cancelReservations(Long resourceId) {
        int cancelledCount = reservationRepository.bulkCancelByResourceId(resourceId);
        log.info("[ReservationBatch] 예약 일괄 취소 완료 - resourceId: {}, count: {}", resourceId, cancelledCount);
        return cancelledCount;
    }

    /**
     * 리소스의 예약 시간 일괄 수정
     * - startDate, endDate 변경
     */
    @Transactional
    public int updateReservationTimes(Long resourceId, LocalDateTime newStartTime, LocalDateTime newEndTime) {
        int updatedCount = reservationRepository.bulkUpdateTimeByResourceId(resourceId, newStartTime, newEndTime);
        log.info("[ReservationBatch] 예약 시간 일괄 수정 완료 - resourceId: {}, count: {}, newTime: {}~{}",
                resourceId, updatedCount, newStartTime, newEndTime);
        return updatedCount;
    }
}