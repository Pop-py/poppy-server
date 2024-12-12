package com.poppy.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 로그인 관리
    UNKNOWN(401, "로그인에 실패하였습니다."),

    // 회원 관리
    DUPLICATE_EMAIL(500, "이미 존재하는 이메일입니다."),
    VERIFICATION_CODE_NOT_FOUND(404, "인증 코드를 찾을 수 없습니다."),
    USER_NOT_FOUND(404, "존재하지 않는 회원입니다."),
    UNAUTHORIZED(403, "인증되지 않은 사용자입니다."),

    // 팝업스토어 조회 관련
    LOCATION_NOT_FOUND(404, "해당 위치를 찾을 수 없습니다."),
    STORE_NOT_FOUND(404, "해당 팝업스토어를 찾을 수 없습니다."),
    INVALID_DATE_RANGE(400, "시작일이 종료일보다 늦을 수 없습니다."),
    SLOT_NOT_FOUND(404, "해당 팝업스토어의 슬롯을 찾을 수 없습니다."),
    INVALID_FORMAT(400, "잘못된 요청 형식입니다."),
    NOT_NULL_PARAMETER(400, "스토어, 날짜, 시간, 인원을 입력해주세요."),

    // 팝업스토어 등록 및 삭제 관련 (관리자)
    INVALID_DATE(500, "존재하는 날짜를 입력해주세요."),
    INVALID_TIME_RANGE(400, "종료 시간은 시작 시간보다 이전일 수 없습니다."),
    INVALID_START_DATE(400, "시작일은 현재 날짜보다 이전일 수 없습니다."),
    STORE_HAS_REFERENCES(500, "예약이나 결제 내역이 존재하여 삭제할 수 없습니다."),

    // 예약 관련
    RESERVATION_NOT_FOUND(404, "예약 정보를 찾을 수 없습니다."),
    RESERVATION_CONFLICT(409, "이미 예약이 진행 중입니다."),
    NO_AVAILABLE_SLOT(400, "이미 예약이 찼습니다."),
    INVALID_RESERVATION_DATE(400, "예약이 불가능한 시간입니다."),
    RESERVATION_FAILED(500, "예약에 실패하였습니다."),
    ALREADY_BOOKED(500, "해당 날짜에 이미 예약이 존재합니다."),
    CANCELLATION_FAILED(500, "예약 취소에 실패하였습니다."),
    INVALID_RESERVATION(500, "예약이 불가한 팝업스토어입니다."),

    // 카테고리 관련
    CATEGORY_NOT_FOUND(400, "카테고리를 찾을 수 없습니다."),

    // 결제 관련
    PAYMENT_FAILED(500, "결제에 실패하였습니다."),
    PAYMENT_NOT_FOUND(404, "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_AMOUNT(500, "결제 금액을 확인해주세요."),
    INVALID_PAYMENT_STATUS(500, "결제 상태를 확인해주세요."),
    PAYMENT_CANCEL_FAILED(500, "결제 취소에 실패하였습니다."),
    ALREADY_PROCESSED_PAYMENT(500, "이미 완료된 결제입니다."),
    ALREADY_CANCELED_PAYMENT(500, "이미 취소된 결제입니다."),

    // 리뷰 관련
    REVIEW_ALREADY_EXISTS(403, "이미 작성한 리뷰가 있습니다."),
    REVIEW_NOT_FOUND(404,"리뷰를 찾을 수 없습니다."),
    NOT_REVIEW_AUTHOR(403,"리뷰 작성자만 삭제/수정이 가능합니다."),

    // 좋아요 관련
    LIKE_NOT_FOUND(404,"좋아요를 찾을 수 없습니다.")
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
