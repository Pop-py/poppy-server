package com.poppy.domain.waiting.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WaitingSettingsReqDto {
    @Min(value = 1, message = "최대 대기 인원은 1명 이상이어야 합니다.")
    @Max(value = 200, message = "최대 대기 인원은 200명을 초과할 수 없습니다.")
    private Integer maxWaitingCount;
}
