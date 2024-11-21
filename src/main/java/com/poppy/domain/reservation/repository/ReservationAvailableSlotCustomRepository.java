package com.poppy.domain.reservation.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationAvailableSlotCustomRepository {
    List<ReservationAvailableSlot> findByPopupStoreAndDate(PopupStore popupStore, LocalDate date);
    List<ReservationAvailableSlot> findByPopupStoreIdAndDateBetween(Long popupStoreId, LocalDate startDate, LocalDate endDate);
    List<ReservationAvailableSlot> findByDateGreaterThanEqualAndPopupStoreStatusEquals(LocalDate date, PopupStoreStatus status);
    Optional<ReservationAvailableSlot> findByPopupStoreIdAndDateAndTime(Long popupStoreId, LocalDate date, LocalTime time);
}
