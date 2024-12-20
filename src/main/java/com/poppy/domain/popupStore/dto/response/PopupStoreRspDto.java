package com.poppy.domain.popupStore.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
import com.poppy.domain.popupStore.entity.PopupStore;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class PopupStoreRspDto {
    private final Long id;

    private final String name;

    private final String description;

    private final String location;        // 상세 위치 설명

    private final String address;         // 실제 도로명 주소

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate startDate;

    @JsonSerialize(using = LocalDateWithDayOfWeekSerializer.class)
    private final LocalDate endDate;

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private final LocalTime openingTime;  // 운영 시작 시간

    @JsonSerialize(using = LocalTimeWithAmPmSerializer.class)
    private final LocalTime closingTime;  // 운영 종료 시간

    private final Integer availableSlot;  // 예약 가능한 총 인원

    private final Boolean isActive;

    private final Boolean isEnd;

    private final Double rating;

    private final String categoryName;

    private final String reservationType;

    private final String thumbnail;

    private final Long price;

    private final String homepageUrl;

    private final String instagramUrl;

    private final String blogUrl;

    private final Integer scrapCount;

    private Boolean isAlmostFull;   // 마감임박 여부

    // Entity to DTO
    public static PopupStoreRspDto from(PopupStore store) {
        return PopupStoreRspDto.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .location(store.getLocation())
                .address(store.getAddress())
                .startDate(store.getStartDate())
                .endDate(store.getEndDate())
                .openingTime(store.getOpeningTime())
                .closingTime(store.getClosingTime())
                .availableSlot(store.getAvailableSlot())
                .isActive(store.getIsActive())
                .isEnd(store.getIsEnd())
                .rating(store.getRating())
                .categoryName(store.getStoreCategory().getName())
                .thumbnail(store.getThumbnail())
                .reservationType(store.getReservationType().toString())
                .price(store.getPrice())
                .homepageUrl(store.getHomepageUrl())
                .instagramUrl(store.getInstagramUrl())
                .blogUrl(store.getBlogUrl())
                .scrapCount(store.getScrapCount())
                .isAlmostFull(store.calculateAlmostFull(store.getReservationAvailableSlots(), store.getReservationType()))
                .build();
    }
}
