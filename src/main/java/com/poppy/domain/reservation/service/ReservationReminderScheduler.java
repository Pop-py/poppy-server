package com.poppy.domain.reservation.service;

import com.poppy.common.config.redis.DistributedLockService;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationReminderScheduler {
    private static final String REMINDER_SCHEDULE = "0 0 * * * *"; // 매시 정각마다 실행

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final DistributedLockService lockService;

    @Scheduled(cron = REMINDER_SCHEDULE)
//    @Transactional(readOnly = true)
//    public void sendReservationReminders() {
//        if (!lockService.tryLock(DistributedLockService.RESERVATION_24H_BEFORE_LOCK)) {
//            log.debug("Failed to acquire reservation reminder lock. Skipping this execution.");
//            return;
//        }
//
//        try {
//            LocalDateTime now = LocalDateTime.now();
//
//            // 예: 현재 시간이 2024-01-10 14:00:00 일 때
//            // 2024-01-11 14:00:00에 예정된 CHECKED 상태의 예약들을 찾음
//            List<Reservation> reservations = reservationRepository.findByDateAndTimeAndStatus(
//                    now.plusDays(1).toLocalDate(),  // 내일 날짜
//                    now.withMinute(0).withSecond(0).withNano(0).toLocalTime(), // 현재 시의 정각
//                    ReservationStatus.CHECKED // 결제 완료된 예약만
//            );
//
//            for (Reservation reservation : reservations) {
//                try {
//                    notificationService.send24HNotification(reservation);
//                    log.info("Sent 24h notification for reservation - id: {}, userId: {}, dateTime: {}",
//                            reservation.getId(),
//                            reservation.getUser().getId(),
//                            reservation.getDate() + " " + reservation.getTime());
//                } catch (Exception e) {
//                    log.error("Failed to send 24h notification for reservation {}: {}",
//                            reservation.getId(), e.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error in reservation reminder scheduler: {}", e.getMessage(), e);
//        } finally {
//            lockService.unlock(DistributedLockService.RESERVATION_24H_BEFORE_LOCK);
//        }
//    }
    @Transactional
    public void sendReservationReminders() {
        if (!lockService.tryLock(DistributedLockService.RESERVATION_24H_BEFORE_LOCK)) {
            log.debug("Failed to acquire reservation reminder lock. Skipping this execution.");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("Current time: {}", now);

            LocalDate targetDate = LocalDate.of(2025, 1, 2);  // 테스트를 위해 하드코딩
            LocalTime targetTime = LocalTime.of(13, 0);      // 테스트를 위해 하드코딩

            log.info("Searching for reservations - date: {}, time: {}, status: {}",
                    targetDate, targetTime, ReservationStatus.CHECKED);

            List<Reservation> reservations = reservationRepository.findByDateAndTimeAndStatus(
                    targetDate,
                    targetTime,
                    ReservationStatus.CHECKED
            );

            log.info("Found {} reservations", reservations.size());

            for (Reservation reservation : reservations) {
                log.info("Processing reservation - id: {}, userId: {}, date: {}, time: {}",
                        reservation.getId(),
                        reservation.getUser().getId(),
                        reservation.getDate(),
                        reservation.getTime());

                notificationService.send24HNotification(reservation);
            }
        } catch (Exception e) {
            log.error("Error in reminder scheduler: {}", e.getMessage(), e);
        } finally {
            lockService.unlock(DistributedLockService.RESERVATION_24H_BEFORE_LOCK);
        }
    }
}
