package com.poppy.domain.scrap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapRspDto {


    @JsonProperty("isScraped")
    private final Boolean isScraped;

    private final Integer scrapCount;

    public static ScrapRspDto of(Boolean isScraped, Integer scrapCount) {
        return ScrapRspDto.builder()
                .isScraped(isScraped)
                .scrapCount(scrapCount)
                .build();
    }
}