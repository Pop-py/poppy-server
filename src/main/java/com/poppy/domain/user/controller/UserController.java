package com.poppy.domain.user.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.user.dto.UserReservationRspDto;
import com.poppy.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    // 유저의 예약 내역 전체 조회
    @GetMapping("/{id}/reservations")
    public RspTemplate<List<UserReservationRspDto>> getReservations(@PathVariable Long id) {
        List<UserReservationRspDto> reservations = userService.getReservations();
        if(reservations.isEmpty()) return new RspTemplate<>(HttpStatus.OK, "예약 내역이 존재하지 않습니다.");

        return new RspTemplate<>(HttpStatus.OK, "예약 내역 조회", reservations);
    }

    // 유저의 예약 내역 상세 조회
    @GetMapping("/{id}/reservations/{reservationId}")
    public RspTemplate<UserReservationRspDto> getReservation(@PathVariable Long id, @PathVariable Long reservationId) {
        UserReservationRspDto reservation = userService.getReservationById(reservationId);
        return new RspTemplate<>(HttpStatus.OK, "예약 내역 상세 조회", reservation);
    }

    // 예약 취소
    @DeleteMapping("/{id}/reservations/{reservationId}")
    public RspTemplate<?> cancelReservation(@PathVariable Long id, @PathVariable Long reservationId) {
        userService.cancelUserReservation(reservationId);
        return new RspTemplate<>(HttpStatus.OK, "예약이 취소되었습니다.");
    }
}
