package com.poppy.domain.review.dto.response;

import com.poppy.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
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
    private Integer likes;  // 좋아요 개수
    private String userName;
    private String popupStoreName;
    private LocalDate date;    // 작성일 (수정 시 해당 날짜로 갱신됨)

    public static ReviewRspDto from(Review review) {
        return ReviewRspDto.builder()
                .id(review.getId())
                .title(review.getTitle())
                .content(review.getContent())
                .thumbnail(review.getThumbnail())
                .rating(review.getRating())
                .likes(review.getReviewLikes().size())
                .userName(review.getUser().getNickname())
                .popupStoreName(review.getPopupStore().getName())
                .date(review.getUpdateTime().toLocalDate())
                .build();
    }
}