package com.poppy.domain.popupStore.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PopupStoreUpdateReqDto {
    @Size(min = 1, max = 255)
    private String name;

    private String description;

    @Size(min = 1, max = 255)
    private String location;

    @Size(min = 1, max = 255)
    private String address;

    private Integer availableSlot;
    private Long price;
    private String homepageUrl;
    private String instagramUrl;
    private String blogUrl;
    private String categoryName;
    private Set<LocalDate> holidays;
    private List<MultipartFile> images;
}