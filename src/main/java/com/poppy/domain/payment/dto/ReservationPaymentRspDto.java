package com.poppy.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ReservationPaymentRspDto {
    private String orderId;       // 주문 ID
    private Long amount;          // 결제 금액
    private String storeName;     // 매장명
    private LocalDate date;       // 예약 날짜
    private LocalTime time;       // 예약 시간
    private int person;           // 예약 인원
}
