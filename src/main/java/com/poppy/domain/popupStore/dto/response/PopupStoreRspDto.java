package com.poppy.domain.popupStore.dto.response;

import com.poppy.domain.popupStore.entity.PopupStore;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupStoreRspDto {
    private final Long id;
    private final String name;
    private final String description;
    private final String location;
    private final String address;


    @Getter
    @Builder
    public static class DateInfo {
        private final int year;
        private final int month;
        private final int day;
    }


    @Getter
    @Builder
    public static class TimeInfo {
        private final int hour;
        private final int minute;
    }

    private final DateInfo startDate;
    private final DateInfo endDate;
    private final TimeInfo openingTime;
    private final TimeInfo closingTime;

    private final Integer availableSlot;
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
    private Boolean isAlmostFull;


    public static PopupStoreRspDto from(PopupStore store) {
        return PopupStoreRspDto.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .location(store.getLocation())
                .address(store.getAddress())
                .startDate(DateInfo.builder()
                        .year(store.getStartDate().getYear())
                        .month(store.getStartDate().getMonthValue())
                        .day(store.getStartDate().getDayOfMonth())
                        .build())
                .endDate(DateInfo.builder()
                        .year(store.getEndDate().getYear())
                        .month(store.getEndDate().getMonthValue())
                        .day(store.getEndDate().getDayOfMonth())
                        .build())
                .openingTime(TimeInfo.builder()
                        .hour(store.getOpeningTime().getHour())
                        .minute(store.getOpeningTime().getMinute())
                        .build())
                .closingTime(TimeInfo.builder()
                        .hour(store.getClosingTime().getHour())
                        .minute(store.getClosingTime().getMinute())
                        .build())
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