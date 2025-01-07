package com.poppy.domain.user.dto.response;

import com.poppy.common.entity.Images;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserPopupStoreRspDto {
    @Getter
    @Builder
    public static class DateInfo {
        private int year;
        private int month;
        private int day;
    }

    private Long userId;
    private Long popupStoreId;
    private String name;
    private String address;
    private UserPopupStoreRspDto.DateInfo startDate;
    private UserPopupStoreRspDto.DateInfo endDate;
    private Boolean isActive;
    private Boolean isEnd;
    private String thumbnailUrl;
    private Boolean isAlmostFull;

    public static UserPopupStoreRspDto of(User user, PopupStore store) {
        List<Images> images = store.getImages();

        return UserPopupStoreRspDto.builder()
                .userId(user.getId())
                .popupStoreId(store.getId())
                .name(store.getName())
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
                .isActive(store.getIsActive())
                .isEnd(store.getIsEnd())
                .thumbnailUrl(images != null && !images.isEmpty() ? images.get(0).getUploadUrl() : null)
                .isAlmostFull(store.calculateAlmostFull(store.getReservationAvailableSlots(), store.getReservationType()))
                .build();
    }
}
