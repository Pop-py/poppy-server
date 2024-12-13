package com.poppy.domain.waiting.entity;


public enum WaitingStatus {
    WAITING("대기 중"),
    CALLED("호출됨"),
    COMPLETED("입장 완료"),
    CANCELED("취소됨");

    private final String description;

    WaitingStatus(String description) {
        this.description = description;
    }
}