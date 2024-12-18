package com.poppy.domain.popupStore.dto.request;

import com.poppy.domain.popupStore.entity.ReservationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupStoreReqDto {
    @NotNull
    @Size(min = 1, max = 255)
    private String name; // 팝업스토어 이름

    @NotNull
    private String description;     // 상세 설명

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

    private Long price;

    private String homepageUrl;

    private String instagramUrl;

    private String blogUrl;

    @NotNull
    private String categoryName; // 카테고리 ID

    @NotNull
    private Long masterUserId; // 관리자 유저 ID

    @NotNull
    private ReservationType reservationType;

    private Set<LocalDate> holidays;

    private String thumbnail; // 썸네일 URL
}
