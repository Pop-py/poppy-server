package com.poppy.domain.popupStore.dto.request;

import com.poppy.domain.popupStore.entity.ReservationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Setter
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
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate startDate; // 시작일

    @NotNull
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate endDate; // 종료일

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openingTime; // 운영 시작 시간

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closingTime; // 운영 종료 시간

    @NotNull
    private Integer availableSlot; // 예약 가능한 총 인원

    private Long price;

    private String homepageUrl;

    private String instagramUrl;

    private String blogUrl;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long masterUserId; // 관리자 유저 ID

    @NotNull
    private ReservationType reservationType;

    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private Set<LocalDate> holidays;

    @NotNull
    private List<MultipartFile> images;     // 이미지 등록 시 여러 개인 경우 첫번째 이미지가 썸네일로 지정됨
}
