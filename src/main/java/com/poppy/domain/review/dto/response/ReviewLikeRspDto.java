package com.poppy.domain.review.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewLikeRspDto {
    private final boolean liked;
    private final Integer likeCount;
}
