package com.poppy.domain.reservation.dto;

import com.poppy.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ReservationRspDto {
    private Long popupStoreId;
    private String popupStoreName;
    private String address;
    private Long userId;
    private LocalDate date;
    private LocalTime time;
    private Integer person;

    public static ReservationRspDto from(Reservation reservation) {
        return ReservationRspDto.builder()
                .popupStoreId(reservation.getPopupStore().getId())
                .popupStoreName(reservation.getPopupStore().getName())
                .address(reservation.getPopupStore().getAddress())
                .userId(reservation.getUser().getId())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .person(reservation.getPerson())
                .build();
    }
}
