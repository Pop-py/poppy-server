package com.poppy.domain.waiting.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurrentWaitingRspDto {
    private final long totalWaiting;
    private final List<WaitingRspDto> nextWaiting;
}
