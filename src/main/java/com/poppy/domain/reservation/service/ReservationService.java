package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.reservation.repository.ReservationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RedissonClient redissonClient;
    private final UserService userService;
    private final PopupStoreRepository popupStoreRepository;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    private final ReservationRepository reservationRepository;
    private final RedisSlotService redisSlotService;

    private static final String LOCK_PREFIX = "reservation:lock:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    // 어플에서 진행하는 예약 메서드
    public Reservation reservation(Long storeId, LocalDate date, LocalTime time) {
        String lockKey = LOCK_PREFIX + storeId + ":" + date + ":" + time;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            // 락을 얻지 못할 경우 예외
            if (!isLocked) throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);

            PopupStore popupStore = popupStoreRepository.findById(storeId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

            User user = userService.getLoggedInUser();  // 로그인 유저 확인

            ReservationAvailableSlot slot = reservationAvailableSlotRepository
                    .findByPopupStoreIdAndDateAndTime(storeId, date, time)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

            // Redis와 DB 상태 동기화
            Integer redisAvailableSlot = redisSlotService.getSlotFromRedis(storeId, date, time);
            if (redisAvailableSlot == null || redisAvailableSlot > slot.getAvailableSlot()) {
                redisSlotService.setSlotToRedis(storeId, date, time, slot.getAvailableSlot());
                redisAvailableSlot = slot.getAvailableSlot();
            }

            // 예약 불가능 확인
            if (redisAvailableSlot <= 0 || slot.getAvailableSlot() <= 0) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }

            // 슬롯 업데이트
            slot.updateSlot();
            if (slot.getAvailableSlot() == 0) {
                slot.updatePopupStatus(PopupStoreStatus.FULL);
            }
            reservationAvailableSlotRepository.save(slot);

            // Redis 업데이트
            redisSlotService.decrementSlot(storeId, date, time);

            // 예약 생성
            Reservation reservation = Reservation.builder()
                    .popupStore(popupStore)
                    .user(user)
                    .date(date)
                    .time(LocalDateTime.of(date, time))
                    .status(ReservationStatus.CHECKED)
                    .build();

            return reservationRepository.save(reservation);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.RESERVATION_FAILED);
        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
