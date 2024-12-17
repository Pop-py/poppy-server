package com.poppy.domain.reservation.entity;

public enum ReservationStatus {
    PENDING("대기"),
    CHECKED("예약 완료"),
    VISITED("방문 완료"),
    CANCELED("예약 취소");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }
}
