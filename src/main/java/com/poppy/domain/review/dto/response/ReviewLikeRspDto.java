package com.poppy.domain.review.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewLikeRspDto {
    private final boolean liked;    
    private final Integer likeCount;

    // 또는 명시적으로 게터 작성
    public boolean isLiked() {
        return liked;
    }

    public Integer getLikeCount() {
        return likeCount;
    }
}
