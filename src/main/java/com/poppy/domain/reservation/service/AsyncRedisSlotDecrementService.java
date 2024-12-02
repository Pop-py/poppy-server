package com.poppy.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncRedisSlotDecrementService {
    private final RedisSlotService redisSlotService;

    @Async
    public void decrementRedisSlot(Long storeId, LocalDate date, LocalTime time, int person) {
        try {
            redisSlotService.decrementSlot(storeId, date, time, person);
        } catch (Exception e) {
            log.error("Redis 슬롯 감소 실패: storeId={}, date={}, time={}, person={}", storeId, date, time, person, e);
        }
    }
}
