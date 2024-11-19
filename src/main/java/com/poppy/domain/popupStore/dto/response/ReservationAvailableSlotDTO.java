package com.poppy.domain.popupStore.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReservationAvailableSlotDTO {
    private String time;          // 시간대 (예: "10:00")
    private int availableSlot;    // 남은 슬롯 수
    private boolean isAvailable;  // 예약 가능 여부


}
