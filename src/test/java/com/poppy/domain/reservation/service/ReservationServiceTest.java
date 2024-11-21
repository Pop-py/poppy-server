package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.reservation.repository.ReservationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
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
    private UserService userService;

    @Mock
    private RedisSlotService redisSlotService;

    @Mock
    private RLock rLock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reservationService = new ReservationService(
                redissonClient,
                userService,
                popupStoreRepository,
                reservationAvailableSlotRepository,
                reservationRepository,
                redisSlotService
        );

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    @Test
    void 슬롯보다_많이_들어올_때_예약_처리() throws InterruptedException {
        // given
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 22);
        LocalTime time = LocalTime.of(19, 0);

        AtomicInteger redisSlot = new AtomicInteger(28); // Redis 슬롯을 원자적 변수로 관리

        PopupStore popupStore = PopupStore.builder().id(storeId).build();
        User user = User.builder().id(1L).build();
        ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .availableSlot(28)
                .totalSlot(28)
                .status(PopupStoreStatus.AVAILABLE)
                .build();

        // Mock 설정
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(userService.getLoggedInUser()).thenReturn(user);
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // Redis 동작 모사
        when(redisSlotService.getSlotFromRedis(storeId, date, time))
                .thenAnswer(inv -> redisSlot.get());

        doAnswer(inv -> {
            int current = redisSlot.decrementAndGet();
            if (current < 0) {
                redisSlot.incrementAndGet();
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }
            return current;
        }).when(redisSlotService).decrementSlot(storeId, date, time);

        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // DB 저장 모사
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger successfulReservations = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.reservation(storeId, date, time);
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
        assertThat(successfulReservations.get()).isEqualTo(28);
        assertThat(redisSlot.get()).isEqualTo(0);
        verify(reservationRepository, times(28)).save(any(Reservation.class));
    }

    @Test
    void 예약_예외_발생_시_Redis_롤백() throws InterruptedException {
        // Arrange
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 28);
        LocalTime time = LocalTime.of(19, 0);

        PopupStore popupStore = PopupStore.builder().id(storeId).build();
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
        when(redisSlotService.getSlotFromRedis(storeId, date, time)).thenReturn(1);
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(userService.getLoggedInUser()).thenReturn(user);
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // DB 작업 중 예외 발생
        doThrow(new BusinessException(ErrorCode.RESERVATION_FAILED))
                .when(reservationAvailableSlotRepository).save(any());

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reservation(storeId, date, time))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.RESERVATION_FAILED.getMessage()); // 예외 메시지 검증

        // Redis 롤백 확인
        verify(redisSlotService).incrementSlot(storeId, date, time);

        // 예약 저장 호출되지 않음
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void 예약_예외_발생_시_DB와_Redis_상태_검증() throws InterruptedException {
        // Arrange
        Long storeId = 2L;
        LocalDate date = LocalDate.of(2024, 12, 1);
        LocalTime time = LocalTime.of(18, 0);

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
        when(userService.getLoggedInUser()).thenReturn(user);
        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.reservation(storeId, date, time))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NO_AVAILABLE_SLOT.getMessage());

        // Redis 롤백 호출되지 않음
        verify(redisSlotService, never()).incrementSlot(storeId, date, time);

        // DB 저장 호출되지 않음
        verify(reservationRepository, never()).save(any());
    }
}
