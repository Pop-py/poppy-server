package com.poppy.domain.reservation.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.QReservationAvailableSlot;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReservationAvailableSlotCustomRepositoryImpl implements ReservationAvailableSlotCustomRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReservationAvailableSlot> findByPopupStoreAndDate(PopupStore popupStore, LocalDate date) {
        QReservationAvailableSlot slot = QReservationAvailableSlot.reservationAvailableSlot;

        return queryFactory.selectFrom(slot)
                .where(slot.popupStore.eq(popupStore).and(slot.date.eq(date)))
                .fetch();
    }

    @Override
    public List<ReservationAvailableSlot> findByPopupStoreIdAndDateBetween(Long popupStoreId, LocalDate startDate, LocalDate endDate) {
        QReservationAvailableSlot slot = QReservationAvailableSlot.reservationAvailableSlot;

        return queryFactory.selectFrom(slot)
                .where(slot.popupStore.id.eq(popupStoreId)
                        .and(slot.date.between(startDate, endDate)))
                .fetch();
    }

    @Override
    public List<ReservationAvailableSlot> findByPopupStoreIdAndStatus(Long popupStoreId, PopupStoreStatus status) {
        QReservationAvailableSlot slot = QReservationAvailableSlot.reservationAvailableSlot;

        return queryFactory.selectFrom(slot)
                .where(slot.popupStore.id.eq(popupStoreId)
                        .and(slot.status.eq(status)))
                .fetch();
    }

    @Override
    public Optional<ReservationAvailableSlot> findByPopupStoreIdAndDateAndTime(Long popupStoreId, LocalDate date, LocalTime time) {
        QReservationAvailableSlot slot = QReservationAvailableSlot.reservationAvailableSlot;

        ReservationAvailableSlot result = queryFactory.selectFrom(slot)
                .where(slot.popupStore.id.eq(popupStoreId)
                        .and(slot.date.eq(date))
                        .and(slot.time.eq(time)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<ReservationAvailableSlot> findByPopupStoreIdAndDateGreaterThanEqualAndStatus(Long popupStoreId, LocalDate date, PopupStoreStatus status) {
        QReservationAvailableSlot slot = QReservationAvailableSlot.reservationAvailableSlot;

        return queryFactory.selectFrom(slot)
                .where(slot.popupStore.id.eq(popupStoreId)
                        .and(slot.date.goe(date))
                        .and(slot.status.eq(status)))
                .fetch();
    }
}
