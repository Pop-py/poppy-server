package com.poppy.domain.scrap.dto;

import com.poppy.common.entity.Images;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.scrap.entity.Scrap;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserScrapRspDto {
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
    
    private Long scrapId;

    private Long popupStoreId;

    private String popupStoreName;

    private String location;

    private String address;

    private DateInfo startDate;

    private DateInfo endDate;

    private String thumbnail;

    private Boolean isAlmostFull;

    private final Integer availableSlot;

    private final Boolean isActive;

    private final Boolean isEnd;

    private final Double rating;

    private final String categoryName;

    private final String reservationType;

    private final Long price;

    private final String homepageUrl;

    private final String instagramUrl;

    private final String blogUrl;

    private final Integer scrapCount;

    private Integer viewCount;

    private int reviewCnt;

    // Entity to DTO
    public static UserScrapRspDto from(Scrap scrap) {
        PopupStore store = scrap.getPopupStore();
        List<Images> images = store.getImages();

        return UserScrapRspDto.builder()
                .scrapId(scrap.getId())
                .popupStoreId(store.getId())
                .popupStoreName(store.getName())
                .location(store.getLocation())
                .address(store.getAddress())
                .startDate(UserScrapRspDto.DateInfo.builder()
                        .year(store.getStartDate().getYear())
                        .month(store.getStartDate().getMonthValue())
                        .day(store.getStartDate().getDayOfMonth())
                        .build())
                .endDate(UserScrapRspDto.DateInfo.builder()
                        .year(store.getEndDate().getYear())
                        .month(store.getEndDate().getMonthValue())
                        .day(store.getEndDate().getDayOfMonth())
                        .build())
                .thumbnail(images != null && !images.isEmpty() ? images.get(0).getUploadUrl() : null)
                .isAlmostFull(store.calculateAlmostFull(
                        store.getReservationAvailableSlots(),
                        store.getReservationType()))
                .availableSlot(store.getAvailableSlot())
                .isActive(store.getIsActive())
                .isEnd(store.getIsEnd())
                .rating(store.getRating())
                .reviewCnt(store.getReviews().size())
                .categoryName(store.getStoreCategory().getName())
                .reservationType(store.getReservationType().toString())
                .price(store.getPrice())
                .homepageUrl(store.getHomepageUrl())
                .instagramUrl(store.getInstagramUrl())
                .blogUrl(store.getBlogUrl())
                .scrapCount(store.getScrapCount())
                .isAlmostFull(store.calculateAlmostFull(store.getReservationAvailableSlots(), store.getReservationType()))
                .viewCount(store.getViews() != null ? store.getViews().size() : 0)
                .build();
    }
}
