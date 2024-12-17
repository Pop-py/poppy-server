package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.payment.dto.ReservationPaymentRspDto;
import com.poppy.domain.payment.entity.Payment;
import com.poppy.domain.payment.entity.PaymentStatus;
import com.poppy.domain.payment.repository.PaymentRepository;
import com.poppy.domain.payment.service.PaymentService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.reservation.repository.ReservationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReservationServiceTest {
    private ReservationService reservationService;

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private PopupStoreRepository popupStoreRepository;
    @Mock
    private ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RedisSlotService redisSlotService;
    @Mock
    private AsyncRedisSlotDecrementService asyncRedisSlotDecrementService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LoginUserProvider loginUserProvider;
    @Mock
    private RLock rLock;

    private Long storeId;
    private LocalDate date;
    private LocalTime time;
    private int person;
    private PopupStore popupStore;
    private User user;
    private ReservationAvailableSlot slot;
    private AtomicInteger redisSlot;

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.openMocks(this);
        reservationService = new ReservationService(
                redissonClient,
                popupStoreRepository,
                reservationAvailableSlotRepository,
                reservationRepository,
                paymentRepository,
                redisSlotService,
                asyncRedisSlotDecrementService,
                paymentService,
                notificationService,
                loginUserProvider
        );

        // 기본 값 설정
        storeId = 1L;
        date = LocalDate.of(2024, 12, 5);
        time = LocalTime.of(14, 0);
        person = 2;
        redisSlot = new AtomicInteger(28);

        // 기본 객체 생성
        popupStore = PopupStore.builder()
                .id(storeId)
                .reservationType(ReservationType.ONLINE)
                .price(5000L)
                .build();

        user = User.builder()
                .id(1L)
                .build();

        slot = ReservationAvailableSlot.builder()
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .availableSlot(28)
                .totalSlot(28)
                .status(PopupStoreStatus.AVAILABLE)
                .build();

        // 기본 Redis Lock 모킹
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        // 기본 Redis Slot 모킹
        when(redisSlotService.getSlotFromRedis(anyLong(), any(), any()))
                .thenAnswer(inv -> redisSlot.get());

        // 기본 Repository 모킹
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));
    }

    @Test
    void 슬롯보다_많이_들어올_때_예약_처리() throws InterruptedException {
        // given
        ConcurrentHashMap<String, Payment> paymentMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, Reservation> reservationMap = new ConcurrentHashMap<>();
        Set<Long> reservedUsers = ConcurrentHashMap.newKeySet();

        doAnswer(inv -> {
            synchronized (redisSlot) {
                int decrementAmount = inv.getArgument(3);
                int currentValue = redisSlot.get();
                if (currentValue < decrementAmount) {
                    throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
                }
                redisSlot.addAndGet(-decrementAmount);
                return null;
            }
        }).when(redisSlotService).decrementSlot(eq(storeId), eq(date), eq(time), anyInt());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    paymentMap.put(payment.getOrderId(), payment);
                    return payment;
                });

        when(paymentRepository.findByOrderId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(paymentMap.get(invocation.getArgument(0))));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    if (reservation.getStatus() == ReservationStatus.PENDING) {
                        reservationMap.put(reservation.getUser().getId(), reservation);
                    }
                    return reservation;
                });

        // 중복 예약 체크
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), eq(storeId), eq(date)))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    return Optional.ofNullable(reservationMap.get(userId));
                });

        // PENDING 상태의 예약 조회
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndStatus(
                anyLong(), eq(storeId), eq(date), eq(ReservationStatus.PENDING)
        )).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return Optional.ofNullable(reservationMap.get(userId));
        });

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger successfulReservations = new AtomicInteger(0);

        for(int i=0; i<100; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    User threadUser = User.builder().id((long) userId).build();
                    when(loginUserProvider.getLoggedInUser()).thenReturn(threadUser);

                    synchronized(reservedUsers) {
                        if(!reservedUsers.contains((long) userId)) {
                            ReservationPaymentRspDto result = reservationService.reservation(storeId, date, time, person);
                            if (result != null) {
                                reservedUsers.add((long) userId);
                                reservationService.completeReservation(result.getOrderId());
                                successfulReservations.incrementAndGet();
                            }
                        }
                    }
                } catch (BusinessException ignored) {
                    // 예약 실패는 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 14개의 성공한 예약에 대해 각각 2번의 save가 호출됨 (임시 예약 + 확정 예약)
        int expectedSaveCalls = 14 * 2;
        assertThat(successfulReservations.get()).isEqualTo(14);
        assertThat(redisSlot.get()).isZero();
        verify(reservationRepository, times(expectedSaveCalls)).save(any(Reservation.class));
        verify(paymentRepository, times(14)).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.PENDING &&
                        payment.getAmount() == person * 5000L
        ));
        verify(redisSlotService, times(14)).decrementSlot(eq(storeId), eq(date), eq(time), eq(person));
    }

    @Test
    void 예약_예외_발생_시_예약과_결제_확인() throws InterruptedException {
        // given
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class)))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_FAILED));

        // when & then
        assertThatThrownBy(() -> reservationService.reservation(storeId, date, time, person))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RESERVATION_FAILED.getMessage());

        verify(reservationRepository, times(1)).save(any());
        verify(paymentRepository, never()).save(any());
        verify(redisSlotService, never()).decrementSlot(any(), any(), any(), anyInt());
        verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, times(1)).isHeldByCurrentThread();
        verify(rLock, times(1)).unlock();
    }

    @Test
    void 동시에_예약과_취소_진행() throws InterruptedException {
        // given
        redisSlot.set(10);  // 초기 슬롯 수 설정
        Long reservationId = 1L;

        Reservation pendingReservation = Reservation.builder()
                .popupStore(popupStore)
                .user(user)
                .date(date)
                .time(time)
                .status(ReservationStatus.PENDING)
                .person(person)
                .build();

        Reservation checkedReservation = Reservation.builder()
                .popupStore(popupStore)
                .user(user)
                .date(date)
                .time(time)
                .status(ReservationStatus.CHECKED)
                .person(person)
                .build();
        ReflectionTestUtils.setField(checkedReservation, "id", reservationId);  // Reflection으로 ID 설정

        Payment mockPayment = Payment.builder()
                .orderId("test-order-id")
                .paymentKey("test-payment-key")
                .status(PaymentStatus.DONE)
                .reservation(checkedReservation)
                .user(user)
                .build();

        // Redis Slot 동작 모킹
        doAnswer(inv -> {
            synchronized (redisSlot) {
                redisSlot.addAndGet(-person);
                return null;
            }
        }).when(redisSlotService).decrementSlot(eq(storeId), eq(date), eq(time), eq(person));

        doAnswer(inv -> {
            synchronized (redisSlot) {
                redisSlot.addAndGet(person);
                return null;
            }
        }).when(redisSlotService).incrementSlot(eq(storeId), eq(date), eq(time), eq(person));

        // Repository 모킹
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), eq(storeId), eq(date)))
                .thenReturn(Optional.empty());

        // 예약 저장 시 ID가 설정된 예약 반환
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> {
                    Reservation savedReservation = inv.getArgument(0);
                    ReflectionTestUtils.setField(savedReservation, "id", reservationId);
                    return savedReservation;
                });

        // 취소를 위한 예약 조회 시 ID가 설정된 예약 반환
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndTime(anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.of(checkedReservation));

        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndStatus(
                anyLong(), eq(storeId), eq(date), eq(ReservationStatus.PENDING)))
                .thenReturn(Optional.of(pendingReservation));

        // 슬롯 저장 모킹
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));
        when(reservationAvailableSlotRepository.save(any(ReservationAvailableSlot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 결제 관련 모킹
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(paymentRepository.findByOrderId(anyString())).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.findByReservationId(eq(reservationId))).thenReturn(Optional.of(mockPayment));
        doNothing().when(paymentService).cancelPayment(eq("test-order-id"), anyString());

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<String> orderId = new AtomicReference<>();

        // 예약 작업 실행
        executorService.submit(() -> {
            try {
                ReservationPaymentRspDto result = reservationService.reservation(storeId, date, time, person);
                orderId.set(result.getOrderId());
                Thread.sleep(100);
                reservationService.completeReservation(result.getOrderId());
            } catch (Exception e) {
                System.out.println("Reservation Exception: " + e);
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        // 예약 취소 작업 실행
        executorService.submit(() -> {
            try {
                Thread.sleep(500);
                if (orderId.get() != null) {
                    reservationService.cancelReservation(user.getId(), storeId, date, time, person);
                }
            } catch (Exception e) {
                // 예외 무시
            } finally {
                latch.countDown();
            }
        });

        // then
        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Redis Lock 검증
        verify(redissonClient, times(2)).getLock(anyString());
        verify(rLock, times(2)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, times(2)).unlock();

        // Redis Slot 작업 검증
        verify(redisSlotService, times(1)).decrementSlot(eq(storeId), eq(date), eq(time), eq(person));
        verify(redisSlotService, times(1)).incrementSlot(eq(storeId), eq(date), eq(time), eq(person));

        // 작업 순서 검증
        InOrder inOrder = inOrder(redisSlotService);
        inOrder.verify(redisSlotService).decrementSlot(eq(storeId), eq(date), eq(time), eq(person));
        inOrder.verify(redisSlotService).incrementSlot(eq(storeId), eq(date), eq(time), eq(person));

        // 최종 상태 검증
        assertThat(redisSlot.get()).isEqualTo(10);
    }

    @Test
    void 결제_완료_시_예약_확정() {
        // given
        String orderId = UUID.randomUUID().toString();  // 고유한 주문 ID 생성

        // Mock 예약 생성
        Reservation pendingReservation = spy(Reservation.builder()
                .popupStore(popupStore)
                .user(user)
                .date(date)
                .time(time)
                .status(ReservationStatus.PENDING)
                .person(person)
                .build());
        doReturn(1L).when(pendingReservation).getId();

        // Mock 결제 생성
        Payment mockPayment = Payment.builder()
                .orderId(orderId)
                .paymentKey("test-payment-key")
                .status(PaymentStatus.PENDING)
                .amount(10000L)
                .reservation(pendingReservation)
                .user(user)
                .build();

        // Repository Mock 설정
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockPayment));
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

        // PENDING 상태의 예약 조회 모킹 추가
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndStatus(
                eq(user.getId()),
                eq(popupStore.getId()),
                eq(date),
                eq(ReservationStatus.PENDING)))
                .thenReturn(Optional.of(pendingReservation));

        // CHECKED 상태의 예약이 없음을 확인하는 모킹 추가
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndStatus(
                eq(user.getId()),
                eq(popupStore.getId()),
                eq(date),
                eq(ReservationStatus.CHECKED)))
                .thenReturn(Optional.empty());

        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(
                eq(popupStore.getId()), eq(date), eq(time))).thenReturn(Optional.of(slot));
        when(reservationAvailableSlotRepository.save(any(ReservationAvailableSlot.class))).thenReturn(slot);

        // Redis 슬롯 관련 모킹
        when(redisSlotService.getSlotFromRedis(anyLong(), any(), any())).thenReturn(10);
        doNothing().when(redisSlotService).decrementSlot(anyLong(), any(), any(), anyInt());

        // when
        ReservationPaymentRspDto reservationResult = reservationService.reservation(storeId, date, time, person);  // 예약 호출

        // 결제 완료
        Reservation confirmedReservation = reservationService.completeReservation(orderId);  // 예약 확정 호출

        // then
        // 예약 결과 검증
        assertThat(reservationResult.getAmount()).isEqualTo(10000L);  // 금액 검증
        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.PENDING &&
                        payment.getAmount() == 10000L
        ));

        // 예약 상태 확인
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CHECKED);

        // Redis Slot 감소 확인
        verify(redisSlotService, times(1)).decrementSlot(eq(storeId), eq(date), eq(time), eq(person));
    }

    @Test
    void 예약_취소_시_결제_취소() {
        // given
        Reservation reservation = Reservation.builder()
                .popupStore(popupStore)
                .user(user)
                .date(date)
                .time(time)
                .status(ReservationStatus.CHECKED)
                .person(person)
                .build();

        Payment payment = Payment.builder()
                .orderId("test-order-id")
                .paymentKey("test-payment-key")
                .status(PaymentStatus.DONE)
                .amount(10000L)
                .reservation(reservation)
                .user(user)
                .build();

        // 예약 및 결제 관련 모의 구현
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndTime(
                user.getId(), popupStore.getId(), date, time))
                .thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservation.getId()))
                .thenReturn(Optional.of(payment));

        // 결제 취소 메서드 모의 구현
        doAnswer(invocation -> {
            // 결제 상태 업데이트
            payment.updateStatus(PaymentStatus.CANCELED);

            return null;
        }).when(paymentService).cancelPayment(eq("test-order-id"), anyString());

        // when
        reservationService.cancelReservation(user.getId(), popupStore.getId(), date, time, person);

        // then
        // 결제 취소 메서드 호출 확인
        verify(paymentService).cancelPayment(eq("test-order-id"), anyString());

        // 예약 상태가 CANCELED로 변경되었는지 확인
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);

        // 결제 상태가 CANCELED로 변경되었는지 확인
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }
}
