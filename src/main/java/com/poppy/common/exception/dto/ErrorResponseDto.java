package com.poppy.common.exception.dto;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ErrorResponseDto<T> {
    private int code;
    private String httpStatus;
    private T errorMessage;

    public ErrorResponseDto(int code, HttpStatus httpStatus, T errorMessage) {
        this.code = code;
        this.httpStatus = httpStatus.getReasonPhrase();
        this.errorMessage = errorMessage;
    }
}
