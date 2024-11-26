package com.poppy.domain.user.dto;

import com.poppy.domain.reservation.entity.Reservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class UserReservationRspDto {
    private String userId;
    private String popupStoreName;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String location;
    private Integer person;

    public static UserReservationRspDto from(Reservation reservation) {
        return UserReservationRspDto.builder()
                .userId(reservation.getUser().getId().toString())
                .popupStoreName(reservation.getPopupStore().getName())
                .reservationDate(reservation.getDate())
                .reservationTime(reservation.getTime())
                .location(reservation.getPopupStore().getLocation())
                .person(reservation.getPerson())
                .build();
    }
}
