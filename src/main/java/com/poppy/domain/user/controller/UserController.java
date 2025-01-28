package com.poppy.domain.user.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.common.auth.dto.TokenRspDto;
import com.poppy.domain.user.dto.response.UserReviewRspDto;
import com.poppy.domain.review.service.ReviewService;
import com.poppy.domain.user.dto.request.UpdateFcmTokenReqDto;
import com.poppy.domain.user.dto.request.UpdateNicknameReqDto;
import com.poppy.domain.user.dto.response.UserPopupStoreRspDto;
import com.poppy.domain.user.dto.response.UserReservationDetailRspDto;
import com.poppy.domain.user.dto.response.UserReservationRspDto;
import com.poppy.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final ReviewService reviewService;

    // 회원가입 후 초기 닉네임 설정
    @PatchMapping("/initial")
    public RspTemplate<TokenRspDto> initialNickname(@RequestParam String code, @Valid @RequestBody UpdateNicknameReqDto reqDto) {
        TokenRspDto tokenRspDto = userService.initialNickname(reqDto.getNickname(), code);
        return new RspTemplate<>(HttpStatus.OK, "회원 가입 완료", tokenRspDto);
    }

    // 유저의 예약 내역 전체 조회
    @GetMapping("/reservations")
    public RspTemplate<List<UserReservationRspDto>> getReservations() {
        List<UserReservationRspDto> reservations = userService.getReservations();
        if(reservations.isEmpty()) return new RspTemplate<>(HttpStatus.OK, "예약 내역이 존재하지 않습니다.");

        return new RspTemplate<>(HttpStatus.OK, "예약 내역 조회", reservations);
    }

    // 유저의 예약 내역 상세 조회
    @GetMapping("/reservations/{reservationId}")
    public RspTemplate<UserReservationDetailRspDto> getReservation(@PathVariable Long reservationId) {
        UserReservationDetailRspDto reservation = userService.getReservationById(reservationId);
        return new RspTemplate<>(HttpStatus.OK, "예약 내역 상세 조회", reservation);
    }

    // 예약 취소
    @DeleteMapping("/reservations/{reservationId}")
    public RspTemplate<Void> cancelReservation(@PathVariable Long reservationId) {
        userService.cancelUserReservation(reservationId);
        return new RspTemplate<>(HttpStatus.OK, "예약이 취소되었습니다.");
    }

    // 닉네임 변경
    @PatchMapping("/{id}")
    public RspTemplate<Void> updateNickname(@PathVariable Long id, @Valid @RequestBody UpdateNicknameReqDto reqDto) {
        userService.updateNickname(reqDto.getNickname());
        return new RspTemplate<>(HttpStatus.OK, reqDto.getNickname() + "으로 변경되었습니다.");
    }

    // 유저별 최근 본 팝업
    @GetMapping("/recent")
    public RspTemplate<List<UserPopupStoreRspDto>> getRecentUserReservations() {
        return new RspTemplate<>(HttpStatus.OK, "최근 본 팝업 조회", userService.getUserPopupStoreRecent());
    }

    // 유저의 리뷰 조회
    @GetMapping("/review")
    public RspTemplate<List<UserReviewRspDto>> getUserReviews() {
        return new RspTemplate<>(HttpStatus.OK, "유저의 리뷰 조회", reviewService.getUserReviews());
    }

    // FCM 토큰 저장
    @PatchMapping("/{id}/fcm-token")
    public RspTemplate<Void> updateFcmToken(
            @PathVariable Long id, @RequestBody UpdateFcmTokenReqDto request, @AuthenticationPrincipal Long authenticatedUserId) {
        userService.updateFcmToken(id, request.getFcmToken(), authenticatedUserId);
        return new RspTemplate<>(HttpStatus.OK, "FCM 토큰이 업데이트되었습니다.");
    }

    // 회원 탈퇴
    @DeleteMapping("/bye")
    public RspTemplate<Void> deleteUser() {
        userService.deleteUser();
        return new RspTemplate<>(HttpStatus.OK, "회원 탈퇴가 완료되었습니다.");
    }
}
