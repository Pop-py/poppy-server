package com.poppy.domain.waiting.dto.request;

import com.poppy.domain.waiting.entity.WaitingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateWaitingStatusReqDto {
    @NotNull(message = "변경할 상태는 필수입니다.")
    private WaitingStatus status;
}
