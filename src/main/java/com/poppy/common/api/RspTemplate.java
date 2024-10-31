package com.poppy.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 응답 템플릿
@Getter
public class RspTemplate<T> {
    private int code;
    private String message;
    private T data;

    public RspTemplate(HttpStatus status, String message) {
        this.code = status.value();
        this.message = message;
    }

    public RspTemplate(HttpStatus status, String message, T data) {
        this.code = status.value();
        this.message = message;
        this.data = data;
    }
}
