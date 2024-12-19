package com.poppy.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationStatus;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime time;

    private String thumbnail;

    private ReservationStatus status;

    private Integer person;

    private Integer price;

    public static ReservationRspDto from(Reservation reservation) {
        return ReservationRspDto.builder()
                .popupStoreId(reservation.getPopupStore().getId())
                .popupStoreName(reservation.getPopupStore().getName())
                .address(reservation.getPopupStore().getAddress())
                .userId(reservation.getUser().getId())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .person(reservation.getPerson())
                .price((int)(reservation.getPopupStore().getPrice() * reservation.getPerson()))
                .build();
    }
}
