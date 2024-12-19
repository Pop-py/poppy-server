package com.poppy.domain.reservation.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.payment.entity.Payment;
import com.poppy.domain.payment.entity.PaymentStatus;
import com.poppy.domain.payment.repository.PaymentRepository;
import com.poppy.domain.payment.service.PaymentService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.payment.dto.ReservationPaymentRspDto;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import com.poppy.domain.reservation.repository.ReservationRepository;
import com.poppy.domain.user.dto.response.UserReservationDetailRspDto;
import com.poppy.domain.user.dto.response.UserReservationRspDto;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RedissonClient redissonClient;
    private final PopupStoreRepository popupStoreRepository;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RedisSlotService redisSlotService;
    private final AsyncRedisSlotDecrementService asyncRedisSlotDecrementService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final LoginUserProvider loginUserProvider;  // 로그인 유저 확인용

    private static final String LOCK_PREFIX = "reservation:lock:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    // 어플에서 진행하는 예약
    @Transactional
    public ReservationPaymentRspDto reservation(Long storeId, LocalDate date, LocalTime time, int person) {
        // 파라미터 예외 처리
        if(storeId == null || date == null || time == null || person <= 0)
            throw new BusinessException(ErrorCode.NOT_NULL_PARAMETER);

        // 팝업 스토어 조회 및 유형 판단
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 예약 유형 확인
        if (popupStore.getReservationType() != ReservationType.ONLINE)
            throw new BusinessException(ErrorCode.INVALID_RESERVATION);

        String lockKey = LOCK_PREFIX + storeId + ":" + date + ":" + time;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Redisson을 이용해 락을 시도
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);

            // 다른 사용자가 이미 락을 획득한 상태일 때
            if(!isLocked) throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);

            // Redis 슬롯 확인
            Integer redisSlot = redisSlotService.getSlotFromRedis(storeId, date, time);
            if (redisSlot == null || redisSlot < person || redisSlot <= 0)
                throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);

            // 로그인 유저 학인
            User user = loginUserProvider.getLoggedInUser();

            // 기존 예약 체크
            Optional<Reservation> existingReservation =
                    reservationRepository.findByUserIdAndPopupStoreIdAndDate(user.getId(), storeId, date);

            // 해당 날짜에 예약이 있는 경우
            if(existingReservation.isPresent())
                throw new BusinessException(ErrorCode.ALREADY_BOOKED);

            // 임시 예약 생성
            Reservation tempReservation = reservationRepository.save(Reservation.builder()
                    .popupStore(popupStore)
                    .user(new User(user.getId()))
                    .date(date)
                    .time(time)
                    .status(ReservationStatus.PENDING)  // 결제 전 상태
                    .person(person)
                    .build()
            );

            String orderId = UUID.randomUUID().toString();
            Long amount = popupStore.getPrice() * person;

            // 결제 생성
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .status(PaymentStatus.PENDING)  // 결제 진행 전
                    .user(user)
                    .reservation(tempReservation)
                    .build();
            paymentRepository.save(payment);

            // 임시 예약 객체 반환
            return ReservationPaymentRspDto.builder()
                    .orderId(orderId)
                    .amount(amount)
                    .storeName(tempReservation.getPopupStore().getName())
                    .date(date)
                    .time(time)
                    .person(person)
                    .build();
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

    // 예약 완료 처리
    @Transactional
    public Reservation completeReservation(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        Reservation tempReservation = payment.getReservation();

        try {
            // Redis 슬롯 감소 시도
            redisSlotService.decrementSlot(
                    tempReservation.getPopupStore().getId(),
                    tempReservation.getDate(),
                    tempReservation.getTime(),
                    tempReservation.getPerson()
            );
        }
        // Redis 슬롯 감소 실패 시 비동기적으로 업데이트 처리
        catch(Exception e) {
            asyncRedisSlotDecrementService.decrementRedisSlot(
                    tempReservation.getPopupStore().getId(),
                    tempReservation.getDate(),
                    tempReservation.getTime(),
                    tempReservation.getPerson()
            );
        }

        // 예약 확정 (DB 업데이트)
        Reservation reservation = processReservation(tempReservation);
        notificationService.sendNotification(reservation, reservation.getStatus());     // 알림 전송
        return reservation;
    }

    // 결제 후 예약 작업 DB 처리
    @Transactional
    public Reservation processReservation(Reservation tempReservation) {
        Long userId = tempReservation.getUser().getId();
        Long storeId = tempReservation.getPopupStore().getId();
        LocalDate date = tempReservation.getDate();
        LocalTime time = tempReservation.getTime();
        int person = tempReservation.getPerson();

        popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        ReservationAvailableSlot slot = reservationAvailableSlotRepository
                .findByPopupStoreIdAndDateAndTime(storeId, date, time)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));

        // 예약 가능한 슬롯 없음
        if (slot.getAvailableSlot() < person || !slot.isAvailable()) throw new BusinessException(ErrorCode.NO_AVAILABLE_SLOT);

        if (slot.getStatus() != PopupStoreStatus.AVAILABLE)
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_DATE);

        // 같은 날짜에 CHECKED 상태의 예약이 있는지 확인
        Optional<Reservation> existingReservation = reservationRepository
                .findByUserIdAndPopupStoreIdAndDateAndStatus(userId, storeId, date, ReservationStatus.CHECKED);
        if (existingReservation.isPresent()) throw new BusinessException(ErrorCode.ALREADY_BOOKED);

        // 슬롯 업데이트
        slot.decreaseSlot(person);
        if (slot.getAvailableSlot() == 0) slot.updatePopupStatus(PopupStoreStatus.FULL);
        reservationAvailableSlotRepository.save(slot);

        // 예약 상태 업데이트
        tempReservation.updateStatus(ReservationStatus.CHECKED);
        return reservationRepository.save(tempReservation);
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

                // 예약 정보 조회
                Reservation reservation = reservationRepository.findByUserIdAndPopupStoreIdAndDateAndTime(
                                userId, storeId, date, time)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

                // 결제 정보 조회 및 결제 취소
                Payment payment = paymentRepository.findByReservationId(reservation.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

                paymentService.cancelPayment(payment.getOrderId(), "고객 예약 취소");

                // 예약 상태 변경
                reservation.updateStatus(ReservationStatus.CANCELED);

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
    public UserReservationDetailRspDto getReservationById(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        return UserReservationDetailRspDto.from(reservation);
    }
}
