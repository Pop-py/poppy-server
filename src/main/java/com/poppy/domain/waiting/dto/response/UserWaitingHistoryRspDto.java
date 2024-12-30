package com.poppy.domain.waiting.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class UserWaitingHistoryRspDto {
    private final Long waitingId;

    private final Integer waitingNumber;

    private final String storeName;

    private final String location;

    private final String phoneNumber;

    private final WaitingStatus status;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate date;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private final LocalTime time;

    public static UserWaitingHistoryRspDto from(Waiting waiting) {
        return UserWaitingHistoryRspDto.builder()
                .waitingId(waiting.getId())
                .waitingNumber(waiting.getWaitingNumber())
                .storeName(waiting.getPopupStore().getName())
                .location(waiting.getPopupStore().getLocation())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .status(waiting.getStatus())
                .date(waiting.getWaitingDate())
                .time(waiting.getWaitingTime())
                .build();
    }
}
