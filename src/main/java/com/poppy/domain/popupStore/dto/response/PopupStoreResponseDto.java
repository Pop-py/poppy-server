package com.poppy.domain.popupStore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class PopupStoreResponseDto {
    private final Long id;
    private final String name;
    private final String location;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalTime time;
    private final Integer availableSlot;
    private final Boolean isActive;
    private final Double rating;
    private final String categoryName;
    private final String thumbnail;
}