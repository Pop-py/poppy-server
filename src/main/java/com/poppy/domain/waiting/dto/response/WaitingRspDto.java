package com.poppy.domain.waiting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class WaitingRspDto {
    private final Long waitingId;
    private final Integer waitingNumber;
    private final String userName;
    private final String phoneNumber;

    @JsonFormat(pattern = "yyyy. MM. dd")
    private final LocalDate createdDate; // 등록 날짜

    @JsonFormat(pattern = "HH:mm")
    private final LocalTime createdTime; // 등록 시간

    public static WaitingRspDto from(Waiting waiting) {
        return WaitingRspDto.builder()
                .waitingId(waiting.getId())
                .waitingNumber(waiting.getWaitingNumber())
                .userName(waiting.getUser().getNickname())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .createdDate(waiting.getCreateTime().toLocalDate())
                .createdTime(waiting.getCreateTime().toLocalTime())
                .build();
    }
}
