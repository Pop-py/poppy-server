package com.poppy.domain.payment.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.payment.entity.Payment;
import com.poppy.domain.payment.entity.PaymentStatus;
import com.poppy.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRepository, tossPaymentClient);
    }

    @Test
    void 결제_성공() {
        // given
        String paymentKey = "test_paymentKey";
        String orderId = "test_orderId";
        Long amount = 10000L;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentClient.confirmPayment(paymentKey, orderId, amount)).thenReturn(true);

        // when
        paymentService.processPayment(paymentKey, orderId, amount);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(payment.getPaymentKey()).isEqualTo(paymentKey);
        verify(tossPaymentClient).confirmPayment(paymentKey, orderId, amount);
    }

    @Test
    void 결제_금액_불일치() {
        // given
        String paymentKey = "test_paymentKey";
        String orderId = "test_orderId";
        Long originalAmount = 10000L;
        Long wrongAmount = 20000L;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(originalAmount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() ->
                paymentService.processPayment(paymentKey, orderId, wrongAmount)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_PAYMENT_AMOUNT.getMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(tossPaymentClient, never()).confirmPayment(anyString(), anyString(), anyLong());
    }

    @Test
    void 결제_실패() {
        // given
        String paymentKey = "test_paymentKey";
        String orderId = "test_orderId";
        Long amount = 10000L;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentClient.confirmPayment(paymentKey, orderId, amount)).thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                paymentService.processPayment(paymentKey, orderId, amount)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_FAILED.getMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void 결제_취소() {
        // given
        String paymentKey = "test_paymentKey";
        String orderId = "test_orderId";
        String cancelReason = "고객 취소";

        Payment payment = Payment.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .status(PaymentStatus.DONE)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentClient.cancelPayment(paymentKey, cancelReason)).thenReturn(true);

        // when
        paymentService.cancelPayment(orderId, cancelReason);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(tossPaymentClient).cancelPayment(paymentKey, cancelReason);
    }

    @Test
    void 결제_취소_실패() {
        // given
        String paymentKey = "test_paymentKey";
        String orderId = "test_orderId";
        String cancelReason = "고객 취소";

        Payment payment = Payment.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .status(PaymentStatus.DONE)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentClient.cancelPayment(paymentKey, cancelReason)).thenReturn(false);

        // when & then
        assertThatThrownBy(() ->
                paymentService.cancelPayment(orderId, cancelReason)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_CANCEL_FAILED.getMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
    }
}