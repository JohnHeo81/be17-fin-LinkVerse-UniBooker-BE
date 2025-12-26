package org.example.unibooker.domain.reservation.model;

/**
 * 리소스 수정 시 예약 처리 액션
 * - CANCEL: 기존 예약 일괄 취소
 * - MODIFY: 기존 예약 변경된 내용으로 수정
 */
public enum ReservationAction {
    CANCEL,
    MODIFY
}