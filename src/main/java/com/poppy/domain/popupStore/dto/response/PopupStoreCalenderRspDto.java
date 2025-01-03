package com.poppy.domain.popupStore.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

// 팝업스토어의 캘린더 (오픈 날짜 ~ 마감 날짜) + 휴무일 반환
@Getter
@Builder
public class PopupStoreCalenderRspDto {
    private final Long id;

    private final String name;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate startDate;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate endDate;

    private Map<LocalDate, PopupStoreStatus> statuses;  // 날짜별 상태 (예약 가능/예약 마감/휴무 등)
}
