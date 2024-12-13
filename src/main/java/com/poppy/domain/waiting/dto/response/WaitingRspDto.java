package com.poppy.domain.waiting.dto.response;

import com.poppy.domain.waiting.entity.Waiting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WaitingRspDto {
    private final Long waitingId;
    private final Integer waitingNumber;
    private final String userName;
    private final String phoneNumber;

    public static WaitingRspDto from(Waiting waiting) {
        return WaitingRspDto.builder()
                .waitingId(waiting.getId())
                .waitingNumber(waiting.getWaitingNumber())
                .userName(waiting.getUser().getNickname())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .build();
    }
}
