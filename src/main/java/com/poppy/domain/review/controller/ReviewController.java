package com.poppy.domain.review.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.review.dto.response.PageRspDto;
import com.poppy.domain.review.entity.ReviewSortType;
import com.poppy.domain.review.dto.request.ReviewReqDto;
import com.poppy.domain.review.dto.response.ReviewLikeRspDto;
import com.poppy.domain.review.dto.response.ReviewRspDto;
import com.poppy.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    // 리뷰 등록
    @PostMapping("/{popupStoreId}")
    public RspTemplate<ReviewRspDto> createReview(
            @PathVariable Long popupStoreId,
            @Valid @RequestBody ReviewReqDto reviewCreateReqDto){
        ReviewRspDto reviewRspDto = reviewService.createReview(popupStoreId,reviewCreateReqDto);

        return new RspTemplate<>(HttpStatus.CREATED, "리뷰 작성 완료", reviewRspDto);
    }

    // 리뷰 수정
    @PatchMapping("/{id}")
    public RspTemplate<ReviewRspDto> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReqDto reviewUpdateReqDto) {
        ReviewRspDto reviewRspDto = reviewService.updateReview(id, reviewUpdateReqDto);
        return new RspTemplate<>(HttpStatus.OK, "리뷰 수정 완료", reviewRspDto);
    }

    // 리뷰 삭제
    @DeleteMapping("/{id}")
    public RspTemplate<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return new RspTemplate<>(HttpStatus.OK, "리뷰 삭제 완료");
    }

    // 리뷰의 좋아요 클릭
    @PostMapping("/{id}/like")
    public RspTemplate<ReviewLikeRspDto> toggleLike(@PathVariable Long id) {
        ReviewLikeRspDto response = reviewService.toggleLike(id);
        return new RspTemplate<>(HttpStatus.OK,
                response.isLiked() ? "좋아요 완료" : "좋아요 취소",
                response);
    }

    // 팝업 스토어의 리뷰 전체 조회 (최근 등록순, 좋아요 많은순, 별점 높은순, 별점 낮은순 정렬)
    @GetMapping("/store/{popupStoreId}")
    public RspTemplate<PageRspDto<ReviewRspDto>> getReviews(
            @PathVariable Long popupStoreId,
            @RequestParam(defaultValue = "RECENT") ReviewSortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ReviewRspDto> reviews = reviewService.getReviews(popupStoreId, sortType, pageRequest);
        PageRspDto<ReviewRspDto> response = new PageRspDto<>(reviews);

        return new RspTemplate<>(HttpStatus.OK, "리뷰 조회 완료", response);
    }

    // 리뷰 상세 조회
    @GetMapping("/{id}")
    public RspTemplate<ReviewRspDto> getReview(@PathVariable Long id) {
        ReviewRspDto review = reviewService.getReview(id);
        return new RspTemplate<>(HttpStatus.OK, "리뷰 조회 완료", review);
    }
}
