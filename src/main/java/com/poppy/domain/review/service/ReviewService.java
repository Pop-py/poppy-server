package com.poppy.domain.review.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.likes.repository.ReviewLikeRepository;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.review.ReviewSortType;
import com.poppy.domain.review.dto.request.ReviewReqDto;
import com.poppy.domain.review.dto.response.ReviewLikeRspDto;
import com.poppy.domain.review.dto.response.ReviewRspDto;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.review.repository.ReviewRepository;
import com.poppy.domain.user.entity.User;

import com.poppy.domain.user.repository.LoginUserProviderImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final LoginUserProviderImpl loginUserProvider;
    private final PopupStoreRepository popupStoreRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    @Transactional
    public ReviewRspDto createReview(Long popupStoreId, ReviewReqDto reviewCreateReqDto) {

        User user = loginUserProvider.getLoggedInUser();
        PopupStore popupStore = popupStoreRepository.findById(popupStoreId).orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 이미 유저가 리뷰를 작성한 경우
        if (reviewRepository.existsByUserAndPopupStore(user, popupStore)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .title(reviewCreateReqDto.getTitle())
                .content(reviewCreateReqDto.getContent())
                .thumbnail(reviewCreateReqDto.getThumbnail())
                .rating(reviewCreateReqDto.getRating())
                .user(user)
                .popupStore(popupStore)
                .build();

        Review savedReview = reviewRepository.save(review);

        return ReviewRspDto.from(savedReview);
    }

    @Transactional
    public ReviewRspDto updateReview(Long reviewId, ReviewReqDto reviewUpdateReqDto) {

        User user = loginUserProvider.getLoggedInUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 리뷰 작성자와 현재 사용자가 같은지 확인
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOT_REVIEW_AUTHOR);
        }

        // 리뷰 수정
        review.update(
                reviewUpdateReqDto.getTitle(),
                reviewUpdateReqDto.getContent(),
                reviewUpdateReqDto.getThumbnail(),
                reviewUpdateReqDto.getRating()
        );

        reviewRepository.save(review);

        return ReviewRspDto.from(review);
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {

        User user = loginUserProvider.getLoggedInUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOT_REVIEW_AUTHOR);
        }

        // 리뷰 삭제
        reviewRepository.delete(review);
    }

    @Transactional
    public ReviewLikeRspDto toggleLike(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        User user = loginUserProvider.getLoggedInUser();

        // 좋아요 여부 확인
        boolean hasLiked = reviewLikeRepository.existsByUserIdAndReviewId(user.getId(), reviewId);

        if (hasLiked) {
            // 좋아요가 있으면 삭제
            reviewLikeRepository.delete(reviewLikeRepository
                    .findByUserAndReview(user, review)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND)));
        } else {
            // 좋아요가 없으면 생성
            ReviewLike newLike = ReviewLike.builder()
                    .user(user)
                    .review(review)
                    .build();
            reviewLikeRepository.save(newLike);
        }

        // 갱신된 좋아요 수 조회
        reviewRepository.flush();
        reviewRepository.refresh(reviewId);

        return ReviewLikeRspDto.builder()
                .liked(!hasLiked)
                .likeCount(review.getReviewLikes().size())
                .build();
    }

    public Page<ReviewRspDto> getReviews(Long popupStoreId, ReviewSortType sortType, PageRequest pageRequest) {
        Page<Review> reviews = switch (sortType) {
            case RECENT -> reviewRepository.findByPopupStoreIdOrderByCreatedAtDesc(popupStoreId, pageRequest);
            case LIKES -> reviewRepository.findByPopupStoreIdOrderByLikeCountDesc(popupStoreId, pageRequest);
            case RATING_HIGH -> reviewRepository.findByPopupStoreIdOrderByRatingDesc(popupStoreId, pageRequest);
            case RATING_LOW -> reviewRepository.findByPopupStoreIdOrderByRatingAsc(popupStoreId, pageRequest);
        };

        return reviews.map(ReviewRspDto::from);
    }
}
