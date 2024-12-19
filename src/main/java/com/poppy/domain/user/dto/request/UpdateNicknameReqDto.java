package com.poppy.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateNicknameReqDto {
    @NotNull(message = "닉네임을 입력해주세요.")
    String nickname;
}
