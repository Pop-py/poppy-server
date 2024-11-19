package com.poppy.domain.reservation.service;

import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RedisTemplate<String, Integer> redisTemplate;
    private final PopupStoreRepository popupStoreRepository;

    //예약 메서드
    //팝업스토어 id
    //slot 이 0 보다 큰지 확인해야하고(아니면 예외)
    //redis 슬롯 처리
    //db 저장




}
