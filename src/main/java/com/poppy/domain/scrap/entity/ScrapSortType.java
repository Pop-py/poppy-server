package com.poppy.domain.scrap.entity;

public enum ScrapSortType {
    RECENT_SAVED("최근 저장일 순"),    // 최근 저장일 순
    OPEN_DATE("오픈일 순"),      // 오픈일 순
    END_DATE("종료일 순")        // 종료일 순
    ;

    ScrapSortType(String description) {}
}
