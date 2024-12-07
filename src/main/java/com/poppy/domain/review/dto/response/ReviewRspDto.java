package com.poppy.domain.review.dto.response;

import com.poppy.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRspDto {
    private Long id;
    private String title;
    private String content;
    private String thumbnail;
    private Double rating;
    private Long userId;
    private String userName;
    private Long popupStoreId;
    private String popupStoreName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewRspDto from(Review review) {
        return ReviewRspDto.builder()
                .id(review.getId())
                .title(review.getTitle())
                .content(review.getContent())
                .thumbnail(review.getThumbnail())
                .rating(review.getRating())
                .userId(review.getUser().getId())
                .userName(review.getUser().getNickname())
                .popupStoreId(review.getPopupStore().getId())
                .popupStoreName(review.getPopupStore().getName())
                .createdAt(review.getCreateTime())
                .updatedAt(review.getUpdateTime())
                .build();
    }
}