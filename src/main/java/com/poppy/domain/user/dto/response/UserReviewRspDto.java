package com.poppy.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.common.entity.Images;
import com.poppy.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserReviewRspDto {
    private Long id;
    private String content;
    private List<String> imageUrls;
    private Double rating;
    private Integer likes;
    private String userName;
    private Long popupStoreId;
    private String popupStoreName;
    private String thumbnailUrl;
    private String location;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    private LocalDate date;

    public static UserReviewRspDto from(Review review) {
        return UserReviewRspDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .imageUrls(review.getImages().stream()
                        .map(Images::getUploadUrl)
                        .collect(Collectors.toList()))
                .rating(review.getRating())
                .likes(review.getReviewLikes().size())
                .userName(review.getUser().getNickname())
                .thumbnailUrl(review.getImages().get(0).getUploadUrl())
                .location(review.getPopupStore().getLocation())
                .popupStoreId(review.getPopupStore().getId())
                .popupStoreName(review.getPopupStore().getName())
                .date(review.getUpdateTime().toLocalDate())
                .build();
    }
}
