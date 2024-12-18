package com.poppy.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
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
