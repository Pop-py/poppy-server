package com.poppy.domain.popupStore.dto.response;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class PopupStoreResponseDto {
    private final Long id;
    private final String name;
    private final String location;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime time;
    private final Integer availableSlot;
    private final Boolean isActive;
    private final Double rating;
    private final String categoryName;

    public PopupStoreResponseDto(Long id, String name, String location, String address,
                                 Double latitude, Double longitude, LocalDate startDate,
                                 LocalDate endDate, LocalDateTime time, Integer availableSlot,
                                 Boolean isActive, Double rating, String categoryName) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.time = time;
        this.availableSlot = availableSlot;
        this.isActive = isActive;
        this.rating = rating;
        this.categoryName = categoryName;
    }
}