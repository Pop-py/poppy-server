package com.poppy.domain.review.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewLikeRspDto {
    private final boolean liked;
    private final Integer likeCount;

    public boolean isLiked() {
        return liked;
    }

    public Integer getLikeCount() {
        return likeCount;
    }
}
