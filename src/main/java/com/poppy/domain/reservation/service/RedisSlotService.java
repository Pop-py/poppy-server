package com.poppy.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisSlotService {
    private final RedisTemplate<String, Integer> redisTemplate;

    // Redis에 슬롯 정보 저장하는 공통 메서드
    public void setSlotToRedis(Long storeId, LocalDate date, LocalTime time, int availableSlot) {
        String slotKey = String.format("slot:%d:%s:%s", storeId, date, time);
        redisTemplate.opsForValue().set(slotKey, availableSlot, 24, TimeUnit.HOURS);
    }

    // Redis에서 슬롯 정보 조회
    public Integer getSlotFromRedis(Long storeId, LocalDate date, LocalTime time) {
        String slotKey = String.format("slot:%d:%s:%s", storeId, date, time);
        return redisTemplate.opsForValue().get(slotKey);
    }

    // Redis의 슬롯 감소
    public void decrementSlot(Long storeId, LocalDate date, LocalTime time) {
        String slotKey = String.format("slot:%d:%s:%s", storeId, date, time);
        redisTemplate.opsForValue().decrement(slotKey);
    }
}
