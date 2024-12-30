package com.poppy.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.poppy.controller..*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MDC.put("requestId", UUID.randomUUID().toString());
        StopWatch stopWatch = new StopWatch();

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            // 정상 실행 로그
            log.info("Method: {}.{}, ExecutionTime: {}ms, Args: {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    stopWatch.getTotalTimeMillis(),
                    Arrays.toString(joinPoint.getArgs())
            );

            return result;

        } catch (Exception e) {
            // 에러 로그
            log.error("Exception in {}.{} with cause = {}, message = {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    e.getCause() != null ? e.getCause() : "NULL",
                    e.getMessage(),
                    e  // 스택트레이스를 로그에 포함
            );
            throw e;
        } finally {
            MDC.clear();
        }
    }
}