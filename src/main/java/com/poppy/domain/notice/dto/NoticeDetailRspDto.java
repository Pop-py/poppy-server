package com.poppy.domain.notice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class NoticeDetailRspDto {
    private String title;
    private String content;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private LocalDate createdDate;
}
