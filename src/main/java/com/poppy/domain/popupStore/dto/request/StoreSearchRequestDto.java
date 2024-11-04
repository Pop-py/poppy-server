package com.poppy.domain.popupStore.dto.request;

import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
public class StoreSearchRequestDto {
    private String keyword;      // 검색어
    private String category;     // 카테고리
    private String location;     // 위치

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; // 시작일

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;   // 종료일
}