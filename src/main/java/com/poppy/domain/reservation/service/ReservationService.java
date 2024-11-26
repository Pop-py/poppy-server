package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.reservation.repository.ReservationRepository;
import com.poppy.domain.user.dto.UserReservationRspDto;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RedissonClient redissonClient;
    private final PopupStoreRepository popupStoreRepository;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    private final ReservationRepository reservationRepository;
    private final RedisSlotService redisSlotService;
    private final LoginUserProvider loginUserProvider;  // 로그인 유저 확인용

    private static final String LOCK_PREFIX = "reservation:lock:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    // 어플에서 진행하는 예약 메서드
    public Reservation reservation(Long storeId, LocalDate date, LocalTime time, int person) {
        // 파라미터 예외 처리
        if(storeId == null || date == null || time == null || person <= 0) {
            throw new BusinessException(ErrorCode.NOT_NULL_PARAMETER);
        }

        // 팝업 스토어 조회 및 유형 판단
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        User user = loginUserProvider.getLoggedInUser();

        // 예약 유형 확인
        if (popupStore.getReservationType() != ReservationType.ONLINE)
            throw new BusinessException(ErrorCode.INVALID_RESERVATION);

        String lockKey = LOCK_PREFIX + storeId + ":" + date + ":" + time;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Redisson을 이용해 락을 시도
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            // 다른 사용자가 이미 락을 획득한 상태일 때
            if (!isLocked) throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);

            // Redis 슬롯 확인
            Integer redisSlot = redisSlotService.getSlotFromRedis(storeId, date, time);
            if (redisSlot == null || redisSlot < person || redisSlot <= 0) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);
            }

            // Redis 업데이트
            redisSlotService.decrementSlot(storeId, date, time, person);

            // DB 작업 처리
            return processReservation(user.getId(), storeId, date, time, person);
        }
        catch (BusinessException e) {
            if (e.getCode() != ErrorCode.NO_AVAILABLE_SLOT.getCode()) {
                redisSlotService.incrementSlot(storeId, date, time, person);
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
    public Reservation processReservation(Long userId, Long storeId, LocalDate date, LocalTime time, int person) {
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        ReservationAvailableSlot slot = reservationAvailableSlotRepository
                .findByPopupStoreIdAndDateAndTime(storeId, date, time)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

        // 예약 가능한 슬롯 없음
        if (slot.getAvailableSlot() < person || !slot.isAvailable()) throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);

        if (slot.getStatus() != PopupStoreStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_DATE);
        }

        // 이미 해당 날짜에 예약한 기록이 있으면 예외
        Optional<Reservation> optionalReservation = reservationRepository.findByUserIdAndPopupStoreIdAndDate(userId, storeId, date);
        if(optionalReservation.isPresent()) throw new BusinessException(ErrorCode.ALREADY_BOOKED);

        // 슬롯 업데이트
        slot.decreaseSlot(person);
        if (slot.getAvailableSlot() == 0) {
            slot.updatePopupStatus(PopupStoreStatus.FULL);
        }
        reservationAvailableSlotRepository.save(slot);

        // 예약 생성
        Reservation reservation = Reservation.builder()
                .popupStore(popupStore)
                .user(new User(userId))
                .date(date)
                .time(time)
                .status(ReservationStatus.CHECKED)
                .person(person)
                .build();

        return reservationRepository.save(reservation);
    }

    // 예약 취소
    @Transactional
    public void cancelReservation(Long userId, Long storeId, LocalDate date, LocalTime time, int person) {
        // 파라미터 체크
        if(storeId == null || date == null || time == null || person <= 0) {
            throw new BusinessException(ErrorCode.NOT_NULL_PARAMETER);
        }

        String lockKey = LOCK_PREFIX + storeId + ":" + date + ":" + time;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);

            // Redis 슬롯 확인
            Integer redisSlot = redisSlotService.getSlotFromRedis(storeId, date, time);
            if (redisSlot == null) {
                throw new BusinessException(ErrorCode.SLOT_NOT_FOUND);
            }

            try {
                // Redis 슬롯 증가 먼저 시도
                redisSlotService.incrementSlot(storeId, date, time, person);

                // DB 작업
                Reservation reservation = reservationRepository.findByUserIdAndPopupStoreIdAndDateAndTime(
                                userId, storeId, date, time)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

                // 예약 삭제
                reservationRepository.delete(reservation);

                // slot 업데이트
                ReservationAvailableSlot slot = reservationAvailableSlotRepository
                        .findByPopupStoreIdAndDateAndTime(storeId, date, time)
                        .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

                slot.increaseSlot(person);
                if (slot.isAvailable() && slot.getStatus() == PopupStoreStatus.FULL) {
                    slot.updatePopupStatus(PopupStoreStatus.AVAILABLE);
                }
                reservationAvailableSlotRepository.save(slot);

            }
            catch (Exception e) {
                // DB 작업 실패시 Redis 롤백
                redisSlotService.decrementSlot(storeId, date, time, person);
                throw e;
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CANCELLATION_FAILED);
        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 유저 별 예약 취소
    public void cancelReservationByReservationId(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        cancelReservation(
                userId,
                reservation.getPopupStore().getId(),
                reservation.getDate(),
                reservation.getTime(),
                reservation.getPerson()
        );
    }

    // 유저의 모든 예약 조회
    @Transactional(readOnly = true)
    public List<UserReservationRspDto> getReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findAllByUserId(userId);
        return reservations.stream()
                .map(UserReservationRspDto::from)
                .collect(Collectors.toList());
    }

    // 유저의 특정 예약 상세 조회
    @Transactional(readOnly = true)
    public UserReservationRspDto getReservationById(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        return UserReservationRspDto.from(reservation);
    }
}
