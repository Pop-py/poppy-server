package com.poppy.domain.reservation.repository;

import com.poppy.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByUserIdAndPopupStoreIdAndDate(Long userId, Long popupStoreId, LocalDate date);
}
