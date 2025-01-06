package com.poppy.admin.service;

import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncRedisSlotInitializationService {
    private final RedisTemplate<String, Integer> redisTemplate;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;

    @Async
    public void initializeRedisSlots(Long popupStoreId) {
        try {
            List<ReservationAvailableSlot> slots = reservationAvailableSlotRepository
                    .findByPopupStoreIdAndDateGreaterThanEqualAndStatus(
                            popupStoreId,
                            LocalDate.now(),
                            PopupStoreStatus.AVAILABLE
                    );

            for (ReservationAvailableSlot slot : slots) {
                String slotKey = String.format("slot:%d:%s:%s",
                        slot.getPopupStore().getId(),
                        slot.getDate(),
                        slot.getTime());

                redisTemplate.opsForValue().set(slotKey, slot.getAvailableSlot(), 365, TimeUnit.DAYS);
            }
        }
        catch (Exception e) {
            log.error("Redis 슬롯 초기화 비동기 작업 실패: storeId={}", popupStoreId, e);
        }
    }

    @Async
    public void clearRedisData(Long popupStoreId) {
        try {
            String pattern = String.format("slot:%d:*", popupStoreId);
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Redis 슬롯 삭제 비동기 작업 실패: storeId={}", popupStoreId, e);
        }
    }
}
