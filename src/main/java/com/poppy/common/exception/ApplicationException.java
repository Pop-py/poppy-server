package com.poppy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 비즈니스 로직이 아닌 외부 환경에서 발생하는 예외
@Getter
public class ApplicationException extends RuntimeException{
    private final int code;
    private final HttpStatus httpStatus;

    // 기본 예외 메시지 출력
    public ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = HttpStatus.valueOf(errorCode.getCode());
    }

    // 상세 예외 메시지 출력
    public ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.httpStatus = HttpStatus.valueOf(errorCode.getCode());
    }
}
