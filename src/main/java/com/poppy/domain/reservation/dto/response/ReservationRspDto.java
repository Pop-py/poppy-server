package com.poppy.domain.reservation.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
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

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private LocalDate date;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
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
