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
public class UserReservationDetailRspDto {
    private Long storeId;

    private String popupStoreName;

    private String address;

    private Long userId;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private LocalDate date;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private LocalTime time;

    private String userNickname;

    private String phoneNumber;

    private String paymentMethod;

    private ReservationStatus status;

    private Integer amount;

    private Integer paidAmount;

    private Integer person;

    public static UserReservationDetailRspDto from(Reservation reservation) {
        return UserReservationDetailRspDto.builder()
                .popupStoreName(reservation.getPopupStore().getName())
                .address(reservation.getPopupStore().getAddress())
                .userId(reservation.getUser().getId())
                .date(reservation.getDate())
                .time(reservation.getTime())
                .userNickname(reservation.getUser().getNickname())
                .phoneNumber(reservation.getUser().getPhoneNumber())
                .paymentMethod("토스페이")
                .amount((int)(reservation.getPerson() * reservation.getPopupStore().getPrice()))
                .paidAmount((int)(reservation.getPerson() * reservation.getPopupStore().getPrice()))
                .person(reservation.getPerson())
                .build();
    }
}