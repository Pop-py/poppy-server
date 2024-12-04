package com.poppy.domain.payment.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;  // 주문 ID (시스템 내부에서 생성)

    @Column(name = "payment_key", unique = true)
    private String paymentKey;  // 토스에서 제공하는 결제 키

    @Column(nullable = false)
    private Long amount;  // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;  // 결제 상태

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 결제한 사용자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;  // 연관된 예약

    @Builder
    public Payment(String orderId, String paymentKey, Long amount, PaymentStatus status, User user, Reservation reservation) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.user = user;
        this.reservation = reservation;
    }

    public void updateStatus(PaymentStatus status) {
        this.status = status;
    }

    public void updatePaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }
}