package com.poppy.domain.review.service;

import com.poppy.common.entity.Images;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.common.service.ImageService;
import com.poppy.domain.likes.repository.ReviewLikeRepository;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.user.dto.response.UserReviewRspDto;
import com.poppy.domain.review.entity.ReviewSortType;
import com.poppy.domain.review.dto.request.ReviewReqDto;
import com.poppy.domain.review.dto.response.ReviewLikeRspDto;
import com.poppy.domain.review.dto.response.ReviewRspDto;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.review.repository.ReviewRepository;
import com.poppy.domain.user.entity.User;

import com.poppy.domain.user.repository.LoginUserProviderImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final String LOCK_PREFIX = "REVIEW_LIKE:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    private final RedissonClient redissonClient;
    private final LoginUserProviderImpl loginUserProvider;
    private final PopupStoreRepository popupStoreRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ImageService imageService;

    // 리뷰 등록
    @Transactional
    public ReviewRspDto createReview(Long popupStoreId, ReviewReqDto reviewCreateReqDto) {
        User user = loginUserProvider.getLoggedInUser();
        PopupStore popupStore = popupStoreRepository.findById(popupStoreId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 이미 유저가 리뷰를 작성한 경우
        if (reviewRepository.existsByUserAndPopupStore(user, popupStore))
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);

        Review review = Review.builder()
                .content(reviewCreateReqDto.getContent())
                .rating(reviewCreateReqDto.getRating())
                .user(user)
                .popupStore(popupStore)
                .build();

        Review savedReview = reviewRepository.save(review);

        // 이미지 업로드 및 저장
        List<Images> uploadedImages = uploadReviewImages(reviewCreateReqDto.getImages(), savedReview);

        // 리뷰에 이미지 추가
        uploadedImages.forEach(savedReview::addImage);

        updatePopupStoreRating(popupStore);

        return ReviewRspDto.from(savedReview);
    }

    // 리뷰 수정
    @Transactional
    public ReviewRspDto updateReview(Long reviewId, ReviewReqDto reviewUpdateReqDto) {
        User user = loginUserProvider.getLoggedInUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 리뷰 작성자와 현재 사용자가 같은지 확인
        if (!review.getUser().getId().equals(user.getId()))
            throw new BusinessException(ErrorCode.NOT_REVIEW_AUTHOR);

        // 새 이미지 업로드 및 저장
        List<Images> newImages;
        if (reviewUpdateReqDto.getImages() != null && !reviewUpdateReqDto.getImages().isEmpty()) {
            newImages = uploadReviewImages(reviewUpdateReqDto.getImages(), review);
        }
        else newImages = new ArrayList<>();

        // 기존 이미지 중 새로 업로드된 이미지에 포함되지 않은 것만 삭제
        List<Images> imagesToDelete = review.getImages().stream()
                .filter(oldImage -> newImages.stream()
                        .noneMatch(newImage ->
                                oldImage.getStoredName().equals(newImage.getStoredName())))
                .toList();

        // 불필요한 이미지 삭제
        imagesToDelete.forEach(image -> imageService.deleteImage(image.getId()));

        // 리뷰 내용, 평점 업데이트 및 새 이미지 설정
        review.update(reviewUpdateReqDto.getContent(), newImages, reviewUpdateReqDto.getRating());

        Review updatedReview = reviewRepository.save(review);

        // 리뷰 갱신
        updatePopupStoreRating(review.getPopupStore());

        // 리뷰 엔티티 저장
        return ReviewRspDto.from(updatedReview);
    }

    // 리뷰 이미지 업로드 메서드
    private List<Images> uploadReviewImages(List<MultipartFile> images, Review review) {
        if (images == null || images.isEmpty()) return new ArrayList<>();

        // 각 이미지를 S3에 업로드하고 URL 반환
        return images.stream()
                .map(image -> imageService.uploadImageFromMultipart(image, "Review", review.getId()))
                .collect(Collectors.toList());
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = loginUserProvider.getLoggedInUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(user.getId()))
            throw new BusinessException(ErrorCode.NOT_REVIEW_AUTHOR);

        // 리뷰에 연결된 모든 이미지 삭제
        review.getImages().forEach(image -> imageService.deleteImage(image.getId()));

        PopupStore popupStore = review.getPopupStore();

        // 리뷰 삭제
        reviewRepository.delete(review);

        // 평균 평점 업데이트
        updatePopupStoreRating(popupStore);
    }

    @Transactional
    public ReviewLikeRspDto toggleLike(Long reviewId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + reviewId);

        try {
            boolean isLocked = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILURE);
            }

            Review review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

            User user = loginUserProvider.getLoggedInUser();

            boolean hasLiked = reviewLikeRepository.existsByUserIdAndReviewId(user.getId(), reviewId);

            if(hasLiked) {
                reviewLikeRepository.delete(reviewLikeRepository
                        .findByUserAndReview(user, review)
                        .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND)));
            } else {
                ReviewLike newLike = ReviewLike.builder()
                        .user(user)
                        .review(review)
                        .build();
                reviewLikeRepository.save(newLike);
            }

            reviewRepository.flush();
            reviewRepository.refresh(reviewId);

            return ReviewLikeRspDto.builder()
                    .liked(!hasLiked)
                    .likeCount(review.getReviewLikes().size())
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILURE);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 팝업 스토어 리뷰 전체 조회
    @Transactional(readOnly = true)
    public Page<ReviewRspDto> getReviews(Long popupStoreId, ReviewSortType sortType, PageRequest pageRequest) {
        // 현재 로그인한 사용자 조회 시도 (로그인하지 않은 경우 null)
        User currentUser = loginUserProvider.getLoggedInUserOrNull();

        // 리뷰 목록 조회
        Page<Review> reviews = switch (sortType) {
            case RECENT -> reviewRepository.findByPopupStoreIdOrderByCreatedAtDesc(popupStoreId, pageRequest);
            case LIKES -> reviewRepository.findByPopupStoreIdOrderByLikeCountDesc(popupStoreId, pageRequest);
            case RATING_HIGH -> reviewRepository.findByPopupStoreIdOrderByRatingDesc(popupStoreId, pageRequest);
            case RATING_LOW -> reviewRepository.findByPopupStoreIdOrderByRatingAsc(popupStoreId, pageRequest);
        };

        // 현재 사용자가 있는 경우 좋아요 상태를 포함하여 변환
        if (currentUser != null) {
            return reviews.map(review -> {
                boolean isLiked = reviewLikeRepository.existsByUserIdAndReviewId(
                        currentUser.getId(),
                        review.getId()
                );
                return ReviewRspDto.of(review, isLiked);
            });
        }

        // 로그인하지 않은 경우 기본 DTO로 변환 (isLiked = false)
        return reviews.map(review -> ReviewRspDto.of(review, false));
    }

    // 리뷰 상세 조회
    @Transactional(readOnly = true)
    public ReviewRspDto getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        return ReviewRspDto.from(review);
    }

    // 해당 팝업스토어의 리뷰 평점 계산
    private void updatePopupStoreRating(PopupStore popupStore) {
        // 해당 팝업스토어의 모든 리뷰 평점의 평균을 계산
        Double averageRating = reviewRepository.calculateAverageRatingByPopupStore(popupStore.getId());
        // 팝업스토어 평점 업데이트
        popupStore.updateAverageRating(averageRating);
        // 변경사항 저장
        popupStoreRepository.save(popupStore);
    }

    // 유저의 리뷰 조회
    @Transactional(readOnly = true)
    public List<UserReviewRspDto> getUserReviews() {
        User user = loginUserProvider.getLoggedInUser();
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
        return reviews.stream()
                .map(UserReviewRspDto::from)
                .collect(Collectors.toList());
    }
}
