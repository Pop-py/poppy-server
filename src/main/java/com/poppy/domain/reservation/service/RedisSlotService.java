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

    // Redis 슬롯 삭제
    public void deleteSlot(Long storeId, LocalDate date, LocalTime time) {
        String key = generateKey(storeId, date, time);
        redisTemplate.delete(key);
    }

    // Redis의 슬롯 감소
    public void decrementSlot(Long storeId, LocalDate date, LocalTime time, int person) {
        String slotKey = String.format("slot:%d:%s:%s", storeId, date, time);
        Long result = redisTemplate.opsForValue().decrement(slotKey, person);

        if (result != null && result < 0) {
            // 슬롯이 음수가 되면 롤백
            redisTemplate.opsForValue().increment(slotKey, person);
            throw new IllegalStateException("Redis 슬롯이 음수가 될 수 없습니다.");
        }
    }

    // Redis의 슬롯 증가
    public void incrementSlot(Long storeId, LocalDate date, LocalTime time, int person) {
        String slotKey = String.format("slot:%d:%s:%s", storeId, date, time);
        redisTemplate.opsForValue().increment(slotKey, person);
    }

    private String generateKey(Long storeId, LocalDate date, LocalTime time) {
        return String.format("reservation:slot:%d:%s:%s", storeId, date, time);
    }
}
