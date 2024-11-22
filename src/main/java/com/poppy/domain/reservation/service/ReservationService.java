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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
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
        // 파라미터 중 하나가 null일 떄 처리
        if(storeId == null || date == null || time == null) {
            throw new BusinessException(ErrorCode.NOT_NULL_PARAMETER);
        }

        String lockKey = LOCK_PREFIX + storeId + ":" + date + ":" + time;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Redisson을 이용해 락을 시도
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            if (!isLocked) {
                throw new BusinessException(ErrorCode.RESERVATION_CONFLICT); // 다른 사용자가 이미 락을 획득한 상태
            }

            // Redis 슬롯 확인
            Integer redisSlot = redisSlotService.getSlotFromRedis(storeId, date, time);
            if (redisSlot == null || redisSlot <= 0) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }

            // Redis 업데이트
            redisSlotService.decrementSlot(storeId, date, time);

            // DB 작업 처리
            Reservation reservation = processReservation(storeId, date, time);

            return reservation;
        }
        catch (BusinessException e) {
            if (e.getCode() != ErrorCode.NO_AVAILABLE_SLOT.getCode()) {
                redisSlotService.incrementSlot(storeId, date, time);
            }
            throw e;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.RESERVATION_FAILED);
        }
        finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // DB 작업 처리
    @Transactional
    protected Reservation processReservation(Long storeId, LocalDate date, LocalTime time) {
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        User user = userService.getLoggedInUser();

        ReservationAvailableSlot slot = reservationAvailableSlotRepository
                .findByPopupStoreIdAndDateAndTime(storeId, date, time)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

        if (slot.getAvailableSlot() <= 0) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT); // 예약 가능한 슬롯 없음
        }

        if (slot.getStatus() != PopupStoreStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_DATE);
        }

        // 이미 해당 날짜에 예약한 기록이 있으면 예외
        Optional<Reservation> optionalReservation = reservationRepository.findByUserIdAndPopupStoreIdAndDate(user.getId(), storeId, date);
        if(optionalReservation.isPresent()) throw new BusinessException(ErrorCode.ALREADY_BOOKED);

        // 슬롯 업데이트
        slot.updateSlot();
        if (slot.getAvailableSlot() == 0) {
            slot.updatePopupStatus(PopupStoreStatus.FULL);
        }
        reservationAvailableSlotRepository.save(slot);

        // 예약 생성
        Reservation reservation = Reservation.builder()
                .popupStore(popupStore)
                .user(user)
                .date(date)
                .time(time)
                .status(ReservationStatus.CHECKED)
                .build();

        return reservationRepository.save(reservation);
    }
}
