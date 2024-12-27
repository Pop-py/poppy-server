package com.poppy.domain.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class NoticeRspDto {
    private Long id;
    private String title;
    private String content;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy.MM.dd")
    private LocalDate createdDate;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="HH:mm")
    private LocalTime createdTime;

    public static NoticeRspDto from(Notice notice) {
        return NoticeRspDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdDate(notice.getCreateTime().toLocalDate())
                .createdTime(notice.getCreateTime().toLocalTime())
                .build();
    }
}
