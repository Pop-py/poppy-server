package com.poppy.domain.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.common.entity.Images;
import com.poppy.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRspDto {
    private Long id;

    private String content;

    private List<String> imageUrls;

    private List<Long> imageIds;

    private Double rating;

    private Integer likes;  // 좋아요 개수

    private String userName;

    private Long popupStoreId;

    private String popupStoreName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    private LocalDate date;    // 작성일 (수정 시 해당 날짜로 갱신됨)

    private Boolean isLiked;

    public static ReviewRspDto from(Review review) {
        return ReviewRspDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .imageUrls(review.getImages().stream()
                        .map(Images::getUploadUrl)
                        .collect(Collectors.toList()))
                .imageIds(review.getImages().stream()
                        .map(Images::getId)
                        .collect(Collectors.toList()))
                .rating(review.getRating())
                .likes(review.getReviewLikes().size())
                .userName(review.getUser().getNickname())
                .popupStoreId(review.getPopupStore().getId())
                .popupStoreName(review.getPopupStore().getName())
                .date(review.getUpdateTime().toLocalDate())
                .build();
    }

    public static ReviewRspDto of(Review review, Boolean isLiked) {
        ReviewRspDto dto = from(review);

        return ReviewRspDto.builder()
                .id(dto.getId())
                .content(dto.getContent())
                .imageUrls(dto.getImageUrls())
                .imageIds(dto.getImageIds())  // 이미지 ID 리스트 추가
                .rating(dto.getRating())
                .likes(dto.getLikes())
                .userName(dto.getUserName())
                .popupStoreId(dto.getPopupStoreId())
                .popupStoreName(dto.getPopupStoreName())
                .date(dto.getDate())
                .isLiked(isLiked)
                .build();
    }
}