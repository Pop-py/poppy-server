package com.poppy.domain.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticeReqDto {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "메시지는 필수입니다.")
    private String content;
}
