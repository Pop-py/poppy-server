package com.poppy.domain.payment.entity;

public enum PaymentStatus {
    PENDING,    // 대기
    DONE,   // 완료
    FAILED, // 실패
    CANCELED    // 취소
}