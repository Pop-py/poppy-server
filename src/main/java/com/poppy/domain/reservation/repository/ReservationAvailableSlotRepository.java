package com.poppy.domain.reservation.repository;

import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationAvailableSlotRepository extends JpaRepository<ReservationAvailableSlot, Long>, ReservationAvailableSlotCustomRepository {
    @Modifying
    @Query("DELETE FROM ReservationAvailableSlot r WHERE r.popupStore.id = :popupStoreId AND r.status = :status")
    void deleteByPopupStoreIdAndStatus(Long popupStoreId, PopupStoreStatus status);
}
