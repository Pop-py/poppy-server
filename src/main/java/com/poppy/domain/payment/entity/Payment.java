package com.poppy.domain.payment.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;  // 주문 ID (시스템 내부에서 생성)

    @Column(name = "payment_key", nullable = false, unique = true)
    private String paymentKey;  // 토스에서 제공하는 결제 키

    @Column(nullable = false)
    private Double amount;  // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;  // 결제 상태: PENDING, COMPLETED, FAILED, CANCELED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 결제한 사용자
}
