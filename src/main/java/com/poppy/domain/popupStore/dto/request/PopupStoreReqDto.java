package com.poppy.domain.popupStore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class PopupStoreReqDto {

    @NotNull
    @Size(min = 1, max = 255)
    private String name; // 팝업스토어 이름

    @NotNull
    @Size(min = 1, max = 255)
    private String location; // 상세 위치 설명

    @NotNull
    @Size(min = 1, max = 255)
    private String address; // 실제 도로명 주소

    @NotNull
    private LocalDate startDate; // 시작일

    @NotNull
    private LocalDate endDate; // 종료일

    @NotNull
    private LocalTime openingTime; // 운영 시작 시간

    @NotNull
    private LocalTime closingTime; // 운영 종료 시간

    @NotNull
    private Integer availableSlot; // 예약 가능한 총 인원

    @NotNull
    private String categoryName; // 카테고리 ID

    @NotNull
    private Long masterUserId; // 관리자 유저 ID

    @NotNull
    private boolean reservationAvailable;

    private String thumbnail; // 썸네일 URL (옵션)

    // 생성자 또는 Builder를 통한 생성
    @Builder
    public PopupStoreReqDto(String name, String location, String address, LocalDate startDate, LocalDate endDate,
                            LocalTime openingTime, LocalTime closingTime, Integer availableSlot, String categoryName,
                            Long masterUserId, String thumbnail ,boolean reservationAvailable) {
        this.name = name;
        this.location = location;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.availableSlot = availableSlot;
        this.categoryName = categoryName;
        this.masterUserId = masterUserId;
        this.thumbnail = thumbnail;
        this.reservationAvailable = reservationAvailable;
    }
}
