package com.poppy.domain.reservation.repository;

import com.poppy.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByUserIdAndPopupStoreIdAndDate(Long userId, Long popupStoreId, LocalDate date);
    Optional<Reservation> findByUserIdAndPopupStoreIdAndDateAndTime(Long userId, Long popupStoreId, LocalDate date, LocalTime time);
    List<Reservation> findAllByUserId(Long userId);
    Optional<Reservation> findByIdAndUserId(Long id, Long userId);
}
