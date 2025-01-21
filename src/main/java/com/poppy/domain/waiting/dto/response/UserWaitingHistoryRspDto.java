package com.poppy.domain.waiting.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.entity.Images;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
public class UserWaitingHistoryRspDto {
    private final Long userId;

    private final String nickname;

    private final Long waitingId;

    private final Integer waitingNumber;

    private final String storeName;

    private final String location;

    private final String address;

    private final String phoneNumber;

    private final WaitingStatus status;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate date;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private final LocalTime time;

    private final String thumbnailUrl;

    public static UserWaitingHistoryRspDto from(Waiting waiting) {
        return UserWaitingHistoryRspDto.builder()
                .userId(waiting.getUser().getId())
                .nickname(waiting.getUser().getNickname())
                .waitingId(waiting.getId())
                .waitingNumber(waiting.getWaitingNumber())
                .storeName(waiting.getPopupStore().getName())
                .location(waiting.getPopupStore().getLocation())
                .address(waiting.getPopupStore().getAddress())
                .phoneNumber(waiting.getUser().getPhoneNumber())
                .status(waiting.getStatus())
                .date(waiting.getWaitingDate())
                .time(waiting.getWaitingTime())
                .thumbnailUrl(
                        Optional.ofNullable(waiting.getPopupStore().getImages())
                                .filter(images -> !images.isEmpty())
                                .map(images -> images.get(0).getUploadUrl())
                                .orElse(null)
                )

                .build();
    }
}
