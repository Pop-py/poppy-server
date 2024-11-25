package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
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
    private RedisSlotService redisSlotService;

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
                redisSlotService,
                loginUserProvider
        );

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    private void mockReservationSetup(Long storeId, Long userId, LocalDate date, LocalTime time, int person) throws InterruptedException {
        PopupStore popupStore = mock(PopupStore.class);
        when(popupStore.getReservationType()).thenReturn(ReservationType.ONLINE);

        ReservationAvailableSlot slot = mock(ReservationAvailableSlot.class);
        when(slot.getAvailableSlot()).thenReturn(10);
        when(slot.getStatus()).thenReturn(PopupStoreStatus.AVAILABLE);

        Reservation reservation = mock(Reservation.class);

        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

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

    @Test
    void 슬롯보다_많이_들어올_때_예약_처리() throws InterruptedException {
        // given
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 22);
        LocalTime time = LocalTime.of(19, 0);
        int person = 2;

        AtomicInteger redisSlot = new AtomicInteger(28);
        AtomicInteger dbSlot = new AtomicInteger(28);

        PopupStore popupStore = PopupStore.builder()
                .id(storeId)
                .reservationType(ReservationType.ONLINE)  // 필수 필드 추가
                .build();

        User user = User.builder().id(1L).build();

        // when절에서 slot 객체가 변경되므로, 매번 새로운 객체를 반환하도록 수정
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenAnswer(inv -> Optional.of(ReservationAvailableSlot.builder()
                        .popupStore(popupStore)
                        .date(date)
                        .time(time)
                        .availableSlot(dbSlot.get())
                        .totalSlot(28)
                        .status(PopupStoreStatus.AVAILABLE)
                        .build()));

        // Mock 설정
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);

        // Redis 동작 모사
        when(redisSlotService.getSlotFromRedis(storeId, date, time))
                .thenAnswer(inv -> redisSlot.get());

        doAnswer(inv -> {
            int currentSlot = redisSlot.get();
            if (currentSlot < person) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }
            redisSlot.addAndGet(-person);
            return null;
        }).when(redisSlotService).decrementSlot(storeId, date, time, person);

        doAnswer(inv -> {
            redisSlot.addAndGet(person);
            return null;
        }).when(redisSlotService).incrementSlot(storeId, date, time, person);

        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        doNothing().when(rLock).unlock();

        // DB 저장 모사
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> {
                    dbSlot.addAndGet(-person);  // DB slot도 감소
                    return inv.getArgument(0);
                });

        // 중복 예약 체크를 위한 mock 설정 추가
        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger successfulReservations = new AtomicInteger(0);

        for (int i=0; i<100; i++) {
            final int userId = i;  // 각 요청마다 다른 사용자로 설정
            executorService.submit(() -> {
                try {
                    // 각 스레드마다 다른 사용자 설정
                    User threadUser = User.builder().id((long) userId).build();
                    when(loginUserProvider.getLoggedInUser()).thenReturn(threadUser);

                    reservationService.reservation(storeId, date, time, person);
                    successfulReservations.incrementAndGet();
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
        int expectedReservations = 28 / person;  // 28명 슬롯에 2명씩 예약하므로 14건
        assertThat(successfulReservations.get()).isEqualTo(expectedReservations);
        assertThat(redisSlot.get()).isZero();
        verify(reservationRepository, times(expectedReservations)).save(any(Reservation.class));
    }

    @Test
    void 예약_예외_발생_시_Redis_롤백() throws InterruptedException {
        // Arrange
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 28);
        LocalTime time = LocalTime.of(19, 0);
        int person = 1;

        PopupStore popupStore = PopupStore.builder()
                .id(storeId)
                .reservationType(ReservationType.ONLINE)
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

        // Mock 설정
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doNothing().when(rLock).unlock();

        // Redis 관련 mock 설정
        when(redisSlotService.getSlotFromRedis(storeId, date, time)).thenReturn(1);
        doNothing().when(redisSlotService).decrementSlot(storeId, date, time, person);
        doNothing().when(redisSlotService).incrementSlot(storeId, date, time, person);

        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);

        when(reservationRepository.findByUserIdAndPopupStoreIdAndDate(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // DB 작업 중 예외 발생
        doThrow(new BusinessException(ErrorCode.RESERVATION_FAILED))
                .when(reservationAvailableSlotRepository).save(any());

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

        // 예약 저장 호출되지 않음
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 예약_예외_발생_시_DB와_Redis_상태_검증() throws InterruptedException {
        // Arrange
        Long storeId = 2L;
        LocalDate date = LocalDate.of(2024, 12, 1);
        LocalTime time = LocalTime.of(18, 0);
        int person = 2;

        PopupStore popupStore = PopupStore.builder().id(storeId).build();
        User user = User.builder().id(2L).build();

        ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .availableSlot(0) // 슬롯 없음
                .totalSlot(10)
                .status(PopupStoreStatus.FULL)
                .build();

        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisSlotService.getSlotFromRedis(storeId, date, time)).thenReturn(0);
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reservation(storeId, date, time, person))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_RESERVATION.getMessage());

        // Redis 롤백 호출되지 않음
        verify(redisSlotService, never()).incrementSlot(storeId, date, time, person);

        // DB 저장 호출되지 않음
        verify(reservationAvailableSlotRepository, never()).save(any());

        // 예약 저장 호출되지 않음
        verify(reservationRepository, never()).save(any());
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

        Future<?> reservationTask = executorService.submit(() -> {
            try {
                reservationService.reservation(storeId, date, time, person);
            } finally {
                latch.countDown();
            }
        });

        Future<?> cancellationTask = executorService.submit(() -> {
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
    }
}
