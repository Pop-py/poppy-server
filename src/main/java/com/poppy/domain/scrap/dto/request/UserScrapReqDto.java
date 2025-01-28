package com.poppy.domain.scrap.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class UserScrapReqDto {
    private List<Long> scrapIds;
}
