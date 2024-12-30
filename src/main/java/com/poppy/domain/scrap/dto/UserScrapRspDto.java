package com.poppy.domain.scrap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.scrap.entity.Scrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserScrapRspDto {
    private Long popupStoreId;

    private String popupStoreName;

    private String location;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM.dd (E)", locale = "ko")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM.dd (E)", locale = "ko")
    private LocalDate endDate;

    private String thumbnail;

    private Boolean isAlmostFull;

    // Entity to DTO
    public static UserScrapRspDto from(Scrap scrap) {
        return UserScrapRspDto.builder()
                .popupStoreId(scrap.getPopupStore().getId())
                .popupStoreName(scrap.getPopupStore().getName())
                .location(scrap.getPopupStore().getLocation())
                .startDate(scrap.getPopupStore().getStartDate())
                .endDate(scrap.getPopupStore().getEndDate())
                .thumbnail(scrap.getPopupStore().getImages().get(0).getUploadUrl())
                .isAlmostFull(scrap.getPopupStore().calculateAlmostFull(
                        scrap.getPopupStore().getReservationAvailableSlots(),
                        scrap.getPopupStore().getReservationType()))
                .build();
    }
}
