package com.poppy.domain.popupStore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

//팝업스토어의 캘린더(오픈 날짜 ~ 마감 날짜) + 휴무일 반환
@Getter
@Builder
public class PopupStoreCalenderDTO {

    private final Long id;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<LocalDate> holidays;

}
