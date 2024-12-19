package com.poppy.domain.user.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime time;

    private String userNickname;

    private String phoneNumber;

    private Integer amount;

    private String paymentMethod;

    private ReservationStatus status;

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