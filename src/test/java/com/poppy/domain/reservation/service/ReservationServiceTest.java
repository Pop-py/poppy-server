package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
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
        // Arrange
        Long storeId = 1L;
        LocalDate date = LocalDate.of(2024, 11, 22);
        LocalTime time = LocalTime.of(19, 0);

        PopupStore popupStore = PopupStore.builder().id(storeId).build();
        User user = User.builder().id(1L).build();

        // 초기 슬롯 설정
        ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                .popupStore(popupStore)
                .date(date)
                .time(time)
                .availableSlot(28) // 제한된 슬롯 수
                .totalSlot(28)
                .status(PopupStoreStatus.AVAILABLE)
                .build();

        // Mock 설정
        when(popupStoreRepository.findById(storeId)).thenReturn(Optional.of(popupStore));
        when(userService.getLoggedInUser()).thenReturn(user);

        when(reservationAvailableSlotRepository.findByPopupStoreIdAndDateAndTime(storeId, date, time))
                .thenReturn(Optional.of(slot));

        // Redis와 DB 상태 동기화 Mock
        when(redisSlotService.getSlotFromRedis(storeId, date, time))
                .thenReturn(28)
                .thenAnswer(invocation -> slot.getAvailableSlot()); // Redis와 DB 상태 동기화

        // Redis 감소 로직 Mock
        doAnswer(invocation -> {
            slot.updateSlot();
            if (slot.getAvailableSlot() == 0) {
                slot.updatePopupStatus(PopupStoreStatus.FULL);
            }
            return null;
        }).when(redisSlotService).decrementSlot(storeId, date, time);

        // 락 Mock
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // 멀티스레드 테스트 준비
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        AtomicInteger successfulReservations = new AtomicInteger(0);

        // Act
        CompletableFuture<Void>[] futures = new CompletableFuture[100];
        for (int i = 0; i < 100; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    reservationService.reservation(storeId, date, time);
                    successfulReservations.incrementAndGet();
                } catch (BusinessException ignored) {
                    // Ignoring exceptions for failed reservations
                }
            }, executorService);
        }

        CompletableFuture.allOf(futures).join();

        // Assert
        assertThat(successfulReservations.get()).isEqualTo(28); // 정확히 28명이 예약 성공
        verify(reservationAvailableSlotRepository, times(28))
                .findByPopupStoreIdAndDateAndTime(storeId, date, time);
        verify(redisSlotService, times(28)).decrementSlot(storeId, date, time);
        verify(reservationRepository, times(28)).save(any(Reservation.class));
        assertThat(slot.getAvailableSlot()).isEqualTo(0); // 슬롯은 0이 되어야 함
        assertThat(slot.getStatus()).isEqualTo(PopupStoreStatus.FULL); // 상태가 FULL로 변경
    }

}
