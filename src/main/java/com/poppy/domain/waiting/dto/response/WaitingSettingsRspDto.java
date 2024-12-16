package com.poppy.domain.waiting.dto.response;

import com.poppy.domain.waiting.entity.WaitingSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WaitingSettingsRspDto {
    private final Long storeId;
    private final Integer maxWaitingCount;

    public static WaitingSettingsRspDto from(WaitingSettings settings) {
        return WaitingSettingsRspDto.builder()
                .storeId(settings.getPopupStore().getId())
                .maxWaitingCount(settings.getMaxWaitingCount())
                .build();
    }
}
