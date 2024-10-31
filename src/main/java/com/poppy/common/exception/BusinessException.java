package com.poppy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 비즈니스 로직 상 예외
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final HttpStatus httpStatus;

    // 기본 에외 메시지
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // throwable 의 detailMessage 에 들어가며, throwable.getMessage()로 부를 수 있음
        this.code = errorCode.getCode();
        this.httpStatus = HttpStatus.valueOf(errorCode.getCode());
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.httpStatus = HttpStatus.valueOf(errorCode.getCode());
    }

    // 상세 예외 메시지
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.code = httpStatus.value();
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.code = httpStatus.value();
        this.httpStatus = httpStatus;
    }
}
