package com.poppy.domain.user.dto.response;

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
public class UserReservationRspDto {
    private String userId;

    private String popupStoreName;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private LocalDate reservationDate;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private LocalTime reservationTime;

    private String location;

    private ReservationStatus status;

    private String thumbnail;

    private Integer person;

    public static UserReservationRspDto from(Reservation reservation) {
        return UserReservationRspDto.builder()
                .userId(reservation.getUser().getId().toString())
                .popupStoreName(reservation.getPopupStore().getName())
                .reservationDate(reservation.getDate())
                .reservationTime(reservation.getTime())
                .location(reservation.getPopupStore().getLocation())
                .status(reservation.getStatus())
                .thumbnail(reservation.getPopupStore().getThumbnail())
                .person(reservation.getPerson())
                .build();
    }
}
