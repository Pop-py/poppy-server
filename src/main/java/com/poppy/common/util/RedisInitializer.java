package com.poppy.common.util;

import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisInitializer implements ApplicationRunner {
    private final RedisTemplate<String, Integer> redisTemplate;
    private final ReservationAvailableSlotRepository slotRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 오늘 이후의 모든 예약 가능 슬롯 조회해서 Redis에 저장
        List<ReservationAvailableSlot> slots = slotRepository
                .findByDateGreaterThanEqualAndPopupStoreStatusEquals(LocalDate.now(), PopupStoreStatus.AVAILABLE);

        for (ReservationAvailableSlot slot: slots) {
            String slotKey = String.format("slot:%d:%s:%s",
                    slot.getPopupStore().getId(),
                    slot.getDate(),
                    slot.getTime());

            redisTemplate.opsForValue().set(slotKey, slot.getAvailableSlot(), 24, TimeUnit.HOURS);
        }
    }
}
