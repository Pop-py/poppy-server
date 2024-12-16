package com.poppy.domain.waiting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserWaitingHistoryRspDto {
    private final Long waitingId;
    private final Integer waitingNumber;
    private final String storeName;
    private final String location;
    private final String phoneNumber;
    private final WaitingStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createTime;

    public static UserWaitingHistoryRspDto from(Waiting waiting) {
        return UserWaitingHistoryRspDto.builder()
                .waitingId(waiting.getId())
                .waitingNumber(waiting.getWaitingNumber())
                .storeName(waiting.getPopupStore().getName())
                .location(waiting.getPopupStore().getLocation())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .status(waiting.getStatus())
                .createTime(waiting.getCreateTime())
                .build();
    }
}
