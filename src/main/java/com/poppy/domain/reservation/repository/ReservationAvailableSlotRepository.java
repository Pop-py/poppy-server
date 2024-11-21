package com.poppy.domain.reservation.repository;

import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationAvailableSlotRepository extends JpaRepository<ReservationAvailableSlot, Long>, ReservationAvailableSlotCustomRepository {
}
