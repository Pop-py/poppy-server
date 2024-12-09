package com.poppy.domain.review.entity;

public enum ReviewSortType {
    RECENT("최근 등록순"),
    LIKES("좋아요 많은순"),
    RATING_HIGH("별점 높은순"),
    RATING_LOW("별점 낮은순");

    private final String description;

    ReviewSortType(String description) {
        this.description = description;
    }
}