package com.poppy.domain.waiting.dto.response;

import com.poppy.domain.waiting.entity.Waiting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyWaitingRspDto {
    private final String userName;
    private final String phoneNumber;

    public static DailyWaitingRspDto from(Waiting waiting) {
        return DailyWaitingRspDto.builder()
                .userName(waiting.getUser().getNickname())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .build();
    }
}
