package com.poppy.domain.payment.repository;

import com.poppy.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByReservationId(Long reservationId);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.reservation.id = :reservationId")
    void deleteByReservationId(@Param("reservationId") Long reservationId);
}
