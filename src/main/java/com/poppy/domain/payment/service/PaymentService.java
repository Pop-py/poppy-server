package com.poppy.domain.payment.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.payment.entity.Payment;
import com.poppy.domain.payment.entity.PaymentStatus;
import com.poppy.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient; // 외부 결제 API 클라이언트

    @Transactional
    public void processPayment(String paymentKey, String orderId, Long amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 중복 결제를 막기 위한 멱등성 검증
        if (payment.getStatus() == PaymentStatus.DONE)
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT);

        // 결제 금액 검증
        if (!amount.equals(payment.getAmount())) {
            payment.updateStatus(PaymentStatus.FAILED);
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 토스페이먼츠 승인 요청
        boolean isSuccess = tossPaymentClient.confirmPayment(paymentKey, orderId, amount);
        if (!isSuccess) {
            payment.updateStatus(PaymentStatus.FAILED);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 결제 상태 업데이트
        payment.updatePaymentKey(paymentKey);
        payment.updateStatus(PaymentStatus.DONE);
    }

    @Transactional
    public void handlePaymentFailure(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 결제 상태 업데이트
        payment.updateStatus(PaymentStatus.FAILED);
    }

    @Transactional
    public void cancelPayment(String orderId, String cancelReason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 취소된 결제인지 확인
        if (payment.getStatus() == PaymentStatus.CANCELED)
            throw new BusinessException(ErrorCode.ALREADY_CANCELED_PAYMENT);

        // 결제가 완료된 상태인지 확인
        if (payment.getStatus() != PaymentStatus.DONE)
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);

        // 토스페이먼츠 결제 취소 요청
        boolean isCanceled = tossPaymentClient.cancelPayment(payment.getPaymentKey(), cancelReason);
        if (!isCanceled) throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);

        // 결제 상태 업데이트
        payment.updateStatus(PaymentStatus.CANCELED);
    }
}
