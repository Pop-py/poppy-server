package com.poppy.domain.waiting.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class DailyWaitingRspDto {
    private final Long userId;
    private final String userName;
    private final String phoneNumber;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate waitingDate;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private final LocalTime waitingTime;

    private final Integer waitingNumber;

    public static DailyWaitingRspDto from(Waiting waiting) {
        return DailyWaitingRspDto.builder()
                .userId(waiting.getUser().getId())
                .userName(waiting.getUser().getNickname())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .waitingDate(waiting.getWaitingDate())
                .waitingTime(waiting.getWaitingTime())
                .waitingNumber(waiting.getWaitingNumber())
                .build();
    }
}