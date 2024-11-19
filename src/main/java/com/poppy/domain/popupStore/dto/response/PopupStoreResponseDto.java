package com.poppy.domain.popupStore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class PopupStoreResponseDto {
    private final Long id;
    private final String name;            // 팝업스토어 이름
    private final String location;        // 상세 위치 설명
    private final String address;         // 실제 도로명 주소
    private final Double latitude;        // 위도
    private final Double longitude;       // 경도
    private final LocalDate startDate;    // 운영 시작일
    private final LocalDate endDate;      // 운영 종료일
    private final LocalTime openingTime;  // 운영 시작 시간
    private final LocalTime closingTime;  // 운영 종료 시간
    private final Integer availableSlot;  // 예약 가능한 총 인원
    private final Boolean isActive;       // 활성화 여부
    private final Boolean isEnd;          // 종료 여부
    private final Double rating;          // 평점
    private final String categoryName;    // 카테고리 이름
    private final String thumbnail;       // 썸네일 이미지 경로
}
