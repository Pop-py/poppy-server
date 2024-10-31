package com.poppy.common.exception.handler;

import com.poppy.common.exception.ApplicationException;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.common.exception.dto.ErrorResponseDto;
import com.poppy.common.util.LoggingUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // BingException 발생 시 (유효성 검사)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto<Map<String, String>>> handleBindException(BindException e, HttpServletRequest request) {
        printLog(e, request);

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();   // 오류 목록 가져오기

        StringBuilder sb = new StringBuilder();
        Map<String, String> errorInfoMap = new HashMap<>();

        // 오류를 추출해서 메시지 담기
        for (FieldError fieldError: fieldErrors) {
            String errorMsg = sb
                    .append(fieldError.getDefaultMessage())
                    .append(". 요청받은 값: ")
                    .append(fieldError.getRejectedValue())
                    .toString();

            errorInfoMap.put(fieldError.getField(), errorMsg);

            sb.setLength(0);
        }

        // 에러 전송 (400 에러)
        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, errorInfoMap);
    }

    // @RequestParam 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto<String>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        printLog(e, request);
        String message = "파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다.";
        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, message);
    }

    // 일반적인 런타임 예외 처리
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorResponseDto<String>> handleBusinessException(RuntimeException e, HttpServletRequest request){
        printLog(e, request);
        return createErrorResponse(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // BusinessException을 상속한 다른 CustomException에도 적용
    @ExceptionHandler({BusinessException.class})
    public ResponseEntity<ErrorResponseDto<String>> handleBusinessException(BusinessException e, HttpServletRequest request){
        printLog(e, request);
        return createErrorResponse(e.getCode(), e.getHttpStatus(), e.getMessage());
    }

    // 비즈니스 로직이 아닌 애플리케이션 서비스 로직상 예외
    @ExceptionHandler({ApplicationException.class})
    public ResponseEntity<ErrorResponseDto<String>> handleAppServiceException(ApplicationException e, HttpServletRequest request){
        printLog(e, request);
        return createErrorResponse(e.getCode(), e.getHttpStatus(), e.getMessage());
    }

    // 예상하지 못한 예외 발생 시 500 에러와 함께 기본 에러 메시지 넘기기
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto<String>> handleException(Exception e, HttpServletRequest request){
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("예외 처리 범위 외의 오류 발생");
        printLog(e, request);
        String fullStackTrace = LoggingUtil.stackTraceToString(e);

        return createErrorResponse(httpStatus.value(), httpStatus, e.getMessage() +", " + fullStackTrace);
    }

    // 응답 생성 메소드
    private <T> ResponseEntity<ErrorResponseDto<T>> createErrorResponse(int statusCode, HttpStatus httpStatus, T errorMessage) {
        ErrorResponseDto<T> errDto = new ErrorResponseDto<>(statusCode, httpStatus, errorMessage);
        return ResponseEntity.status(httpStatus).body(errDto);
    }

    // ErrorCode를 받아서 상태 코드와 메시지를 사용해 응답을 생성
    private ResponseEntity<ErrorResponseDto<String>> createErrorResponse(ErrorCode errorCode) {
        int statusCode = errorCode.getCode();
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        ErrorResponseDto<String> errDto = new ErrorResponseDto<>(
                statusCode, httpStatus, errorCode.getMessage());
        return ResponseEntity.status(httpStatus).body(errDto);
    }

    // 예외 출력
    private void printLog(Exception e, HttpServletRequest request) {
        log.error("발생 예외: {}, 에러 메시지: {}, 요청 Method: {}, 요청 url: {}",
                e.getClass().getSimpleName(), e.getMessage(), request.getMethod(), request.getRequestURI(), e);
    }
}
