package com.poppy.domain.popupStore.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.poppy.common.util.LocalDateWithDayOfWeekSerializer;
import com.poppy.common.util.LocalTimeWithAmPmSerializer;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.ReservationType;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
                .isAlmostFull(calculateAlmostFull(store.getReservationAvailableSlots(), store.getReservationType()))
                .build();
    }

    // 마감임박 여부 판단
    private static Boolean calculateAlmostFull(List<ReservationAvailableSlot> slots, ReservationType reservationType) {
        // Offline인 경우 null
        if(reservationType == ReservationType.OFFLINE) return null;

        if(slots == null || slots.isEmpty()) return false;

        // 현재 시점 이후의 휴무일이 아닌 슬롯만 필터링
        List<ReservationAvailableSlot> activeSlots = slots.stream()
                .filter(slot -> !slot.getStatus().equals(PopupStoreStatus.HOLIDAY))
                .filter(slot -> {
                    LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getTime());
                    return !slotDateTime.isBefore(LocalDateTime.now());
                })
                .toList();

        if (activeSlots.isEmpty()) return false;

        // 남은 슬롯 수와 전체 슬롯 수 계산
        int remainingSlots = activeSlots.stream()
                .mapToInt(ReservationAvailableSlot::getAvailableSlot)
                .sum();

        int totalSlots = activeSlots.stream()
                .mapToInt(ReservationAvailableSlot::getTotalSlot)
                .sum();

        // 전체 슬롯의 20% 이하가 남은 경우 true
        return totalSlots > 0 && ((double) remainingSlots / totalSlots) <= 0.2;
    }
}
