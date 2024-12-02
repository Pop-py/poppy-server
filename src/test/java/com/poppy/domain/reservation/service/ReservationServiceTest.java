package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
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
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private PaymentService paymentService;

    @Mock
    private LoginUserProvider loginUserProvider;

    @Mock
    private RLock rLock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reservationService = new ReservationService(
                redissonClient,
                popupStoreRepository,
                reservationAvailableSlotRepository,
                reservationRepository,
                paymentRepository,
                redisSlotService,
                paymentService,
                loginUserProvider
        );

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    private void mockReservationSetup(Long storeId, Long userId, LocalDate date, LocalTime time, int person) throws InterruptedException {
        PopupStore popupStore = mock(PopupStore.class);
        when(popupStore.getReservationType()).thenReturn(ReservationType.ONLINE);
        when(popupStore.getPrice()).thenReturn(5000L);

        ReservationAvailableSlot slot = mock(ReservationAvailableSlot.class);
        when(slot.getAvailableSlot()).thenReturn(10);
        when(slot.getStatus()).thenReturn(PopupStoreStatus.AVAILABLE);

        Reservation reservation = mock(Reservation.class);

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

        Payment payment = Payment.builder()
                .orderId(UUID.randomUUID().toString())
                .status(PaymentStatus.PENDING)
                .amount((long) person * 5000)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(new User(userId));
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time)).thenReturn(Optional.of(slot));
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(userId, storeId, date)).thenReturn(Optional.empty());

        // Redis 설정
        when(redisSlotService.getSlotFromRedis(storeId, date, time)).thenReturn(10);
        doNothing().when(redisSlotService).decrementSlot(storeId, date, time, person);
        doNothing().when(redisSlotService).incrementSlot(storeId, date, time, person);

        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDateAndTime(userId, storeId, date, time)).thenReturn(Optional.of(reservation));
        doNothing().when(reservationRepository).delete(reservation);
    }

    private Payment mockPaymentSetup(String orderId, PaymentStatus status) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .status(status)
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        return payment;
    }

    @Test
    void 슬롯보다_많이_들어올_때_예약_처리() throws InterruptedException {
        // given
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 22);
        LocalTime time = LocalTime.of(19, 0);
        int person = 2;

        AtomicInteger redisSlot = new AtomicInteger(28);
        ConcurrentHashMap<String, Payment> paymentMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, Reservation> reservationMap = new ConcurrentHashMap<>();
        Set<Long> reservedUsers = ConcurrentHashMap.newKeySet();

        PopupStore popupStore = PopupStore.builder()
                .id(storeId)
                .reservationType(ReservationType.ONLINE)
                .price(5000L)
                .build();

        // Redis 관련 모킹
        when(redisSlotService.getSlotFromRedis(storeId, date, time))
                .thenAnswer(inv -> redisSlot.get());

        doAnswer(inv -> {
            redisSlot.set(28);
            return null;
        }).when(redisSlotService).setSlotToRedis(eq(storeId), eq(date), eq(time), eq(28));

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

        // Redis 키 초기화
        redisSlotService.setSlotToRedis(storeId, date, time, 28);

        try {
            when(popupStoreRepository.findById(storeId))
                    .thenReturn(Optional.of(popupStore));

            ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                    .popupStore(popupStore)
                    .date(date)
                    .time(time)
                    .availableSlot(28)
                    .totalSlot(28)
                    .status(PopupStoreStatus.AVAILABLE)
                    .build();
            when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                    .thenReturn(Optional.of(slot));

            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(true);

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
                        Reservation reservation = reservationMap.get(userId);
                        return Optional.ofNullable(reservation);
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
                    } catch (BusinessException e) {
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
        } finally {
            redisSlotService.deleteSlot(storeId, date, time);
        }
    }

    @Test
    void 예약_예외_발생_시_Redis_롤백() throws InterruptedException {
        // Arrange
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 28);
        LocalTime time = LocalTime.of(19, 0);
        int person = 1;

        AtomicInteger redisSlot = new AtomicInteger(1);

        PopupStore popupStore = PopupStore.builder()
                .id(storeId)
                .reservationType(ReservationType.ONLINE)
                .price(5000L)
                .build();

        User user = User.builder().id(1L).build();

        ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .availableSlot(1)
                .totalSlot(1)
                .status(PopupStoreStatus.AVAILABLE)
                .build();

        // Redis 초기화 설정
        doAnswer(inv -> {
            redisSlot.set(1); // 초기 슬롯 수량
            return null;
        }).when(redisSlotService).setSlotToRedis(eq(storeId), eq(date), eq(time), eq(1));

        redisSlotService.setSlotToRedis(storeId, date, time, 1); // Redis 키 초기화

        when(redisSlotService.getSlotFromRedis(storeId, date, time))
                .thenReturn(redisSlot.get());

        doAnswer(inv -> {
            synchronized (redisSlot) {
                int decrementAmount = inv.getArgument(3);
                int currentValue = redisSlot.get();
                if (currentValue < decrementAmount) {
                    throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
                }
                redisSlot.addAndGet(-decrementAmount);
                System.out.println("슬롯 감소 후 남은 수량: " + redisSlot.get());
                return null;
            }
        }).when(redisSlotService).decrementSlot(eq(storeId), eq(date), eq(time), anyInt());

        // Lock 설정
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);

        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // Reservation 객체 생성 및 Mock 설정 수정
        Reservation tempReservation = Reservation.builder()
                .user(user)
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .person(person)
                .status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.save(any(Reservation.class))).thenReturn(tempReservation);

        // 예외 발생을 위한 ReservationAvailableSlot 저장 시 설정
        when(reservationAvailableSlotRepository.save(any(ReservationAvailableSlot.class)))
                .thenAnswer(invocation -> {
                    ReservationAvailableSlot slotToSave = invocation.getArgument(0);
                    if (slotToSave.getAvailableSlot() < 1) {
                        throw new BusinessException(ErrorCode.RESERVATION_FAILED);
                    }
                    return slotToSave;
                });

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reservation(storeId, date, time, person))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RESERVATION_FAILED.getMessage());

        // 검증
        InOrder inOrder = inOrder(redisSlotService, rLock, reservationAvailableSlotRepository);

        // Lock 획득 확인
        inOrder.verify(rLock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));

        // Redis 작업 확인
        inOrder.verify(redisSlotService).getSlotFromRedis(storeId, date, time);
        inOrder.verify(redisSlotService).decrementSlot(storeId, date, time, person);

        // DB 작업 확인
        inOrder.verify(reservationAvailableSlotRepository).findByPopupStoreIdAndDateAndTime(storeId, date, time);
        inOrder.verify(reservationAvailableSlotRepository).save(any());

        // Redis 롤백 확인
        verify(redisSlotService).incrementSlot(storeId, date, time, person);

        // Lock 해제 확인
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();

        // 예약 저장 호출 확인
        verify(reservationRepository, times(1)).save(any());

        // 결제 저장 호출되지 않음 확인
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 동시에_예약과_취소가_진행() throws Exception {
        // given
        Long storeId = 1L;
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.of(11, 0);
        int person = 2;

        AtomicInteger redisSlot = new AtomicInteger(10); // Redis 초기값
        AtomicInteger dbSlot = new AtomicInteger(10);    // DB 초기값

        mockReservationSetup(storeId, userId, date, time, person);

        when(redisSlotService.getSlotFromRedis(storeId, date, time)).thenAnswer(invocation -> redisSlot.get());
        doAnswer(invocation -> {
            if (redisSlot.get() < person) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }
            redisSlot.addAndGet(-person);
            return null;
        }).when(redisSlotService).decrementSlot(storeId, date, time, person);

        doAnswer(invocation -> {
            redisSlot.addAndGet(person);
            return null;
        }).when(redisSlotService).incrementSlot(storeId, date, time, person);

        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenAnswer(invocation -> Optional.of(ReservationAvailableSlot.builder()
                        .availableSlot(dbSlot.get())
                        .status(PopupStoreStatus.AVAILABLE)
                        .build()));

        doAnswer(invocation -> {
            dbSlot.addAndGet(-person);
            return null;
        }).when(reservationAvailableSlotRepository).save(any(ReservationAvailableSlot.class));

        // when
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.submit(() -> {
            try {
                reservationService.reservation(storeId, date, time, person);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                reservationService.cancelReservation(userId, storeId, date, time, person);
            } finally {
                latch.countDown();
            }
        });

        latch.await(); // 두 작업 완료 대기
        executorService.shutdown();

        // then
        verify(redisSlotService, atLeastOnce()).decrementSlot(storeId, date, time, person);
        verify(redisSlotService, atLeastOnce()).incrementSlot(storeId, date, time, person);
        verify(reservationAvailableSlotRepository, atLeastOnce()).save(any(ReservationAvailableSlot.class));
        verify(reservationRepository, atLeastOnce()).save(any(Reservation.class));
        verify(reservationRepository, atLeastOnce()).delete(any(Reservation.class));
        verify(paymentService, atLeastOnce()).cancelPayment(anyString(), anyString());
        verify(paymentRepository, atLeastOnce()).save(argThat(p ->
                p.getStatus() == PaymentStatus.CANCELED
        ));
    }

    @Test
    void 결제_완료_시_예약_확정() throws InterruptedException {
        // given
        Long storeId = 1L;
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.of(11, 0);
        int person = 2;
        String orderId = UUID.randomUUID().toString();

        mockReservationSetup(storeId, userId, date, time, person);
        PopupStore popupStore = PopupStore.builder()
                .id(storeId)
                .price(5000L)
                .build();
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));

        // when
        ReservationPaymentRspDto reservationResult = reservationService.reservation(storeId, date, time, person);

        // then
        assertThat(reservationResult.getAmount()).isEqualTo(10000L);
        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.PENDING &&
                        payment.getAmount() == 10000L
        ));

        // 결제 완료 시
        Reservation confirmedReservation = reservationService.completeReservation(orderId);
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CHECKED);
    }

    @Test
    void 결제_실패_시_예약_취소() {
        // given
        String orderId = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .orderId(orderId)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        assertThatThrownBy(() -> reservationService.completeReservation(orderId))
                .isInstanceOf(BusinessException.class);

        // when & then
        assertThatThrownBy(() -> {
            reservationService.completeReservation(orderId);
        }).isInstanceOf(BusinessException.class);

        verify(reservationRepository).delete(payment.getReservation());
    }

    @Test
    void 예약_취소_시_결제_취소() throws InterruptedException {
        // given
        Long storeId = 1L;
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.of(11, 0);
        int person = 2;
        String orderId = UUID.randomUUID().toString();

        mockReservationSetup(storeId, userId, date, time, person);
        Payment payment = mockPaymentSetup(orderId, PaymentStatus.DONE);
        when(paymentRepository.findByReservationId(any())).thenReturn(Optional.of(payment));

        // when
        reservationService.cancelReservation(userId, storeId, date, time, person);

        // then
        verify(paymentService).cancelPayment(orderId, "고객 예약 취소");
        verify(paymentRepository).save(argThat(p ->
                p.getStatus() == PaymentStatus.CANCELED
        ));
    }
}
