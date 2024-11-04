package com.poppy.common.util;

import com.google.gson.Gson;
import com.poppy.common.exception.ErrorCode;
import com.poppy.common.exception.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

@Slf4j
public class ErrorResponder {   // 필터나 인터셉터에서 예외 응답 제공
    public static void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, Exception e) throws IOException {
        printLog(e, request);   // 에러 출력

        Gson gson = new Gson();
        ErrorResponseDto<String> responseDto = createResponseDto(errorCode);

        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);  // json
        response.setStatus(errorCode.getCode());    // 상태 코드
        response.getWriter().write(gson.toJson(responseDto, ErrorResponseDto.class));   // 응답 객체
    }

    public static ErrorResponseDto<String> createResponseDto(ErrorCode errorCode) {
        int code = errorCode.getCode();
        HttpStatus httpStatus = HttpStatus.valueOf(code);

        return new ErrorResponseDto<>(code, httpStatus, errorCode.getMessage());
    }

    private static void printLog(Exception e, HttpServletRequest request) {
        // 헤더가 비어 있는 경우 (NullAuthorizationException)
        if(e == null)
            log.error("발생 예외: {}, 에러 메시지: {}, 요청 Method: {}, 요청 url: {}",
                    "NullAuthorizationException", "Authorization Header가 비어있습니다.", request.getMethod(), request.getRequestURI());

        else
            log.error("발생 예외: {}, 에러 메시지: {}, 요청 Method: {}, 요청 url: {}",
                    e.getClass().getSimpleName(), e.getMessage(), request.getMethod(), request.getRequestURI(), e);
    }
}
