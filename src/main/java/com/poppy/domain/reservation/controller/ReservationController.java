package com.poppy.domain.reservation.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.reservation.dto.ReservationReqDto;
import com.poppy.domain.reservation.dto.ReservationRspDto;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public RspTemplate<ReservationRspDto> reservation(@Valid @RequestBody ReservationReqDto reservationReqDto) {
        Reservation reservation = reservationService.reservation(
                reservationReqDto.getPopupStoreId(),
                reservationReqDto.getDate(),
                reservationReqDto.getTime()
        );
        return new RspTemplate<>(HttpStatus.OK, "예약 완료", ReservationRspDto.from(reservation));
    }

    @DeleteMapping
    public RspTemplate<Void> deleteReservation(@Valid @RequestBody ReservationReqDto reservationReqDto) {
        reservationService.cancelReservation(
                reservationReqDto.getPopupStoreId(),
                reservationReqDto.getDate(),
                reservationReqDto.getTime()
        );
        return new RspTemplate<>(HttpStatus.OK, "예약 취소 완료");
    }
}
