package com.poppy.domain.review.service;

import com.poppy.common.entity.Images;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.common.service.ImageService;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.likes.repository.ReviewLikeRepository;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.review.entity.ReviewSortType;
import com.poppy.domain.review.dto.request.ReviewReqDto;
import com.poppy.domain.review.dto.response.ReviewLikeRspDto;
import com.poppy.domain.review.dto.response.ReviewRspDto;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.review.repository.ReviewRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private LoginUserProviderImpl loginUserProvider;
    @Mock
    private PopupStoreRepository popupStoreRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewLikeRepository reviewLikeRepository;
    @Mock
    private ImageService imageService;
    @Mock
    private RedissonClient redissonClient;

    private User testUser;
    private PopupStore testStore;
    private Review testReview;
    private Images testImage;
    private RLock mockLock;

    @BeforeEach
    void setUp() {
        mockLock = mock(RLock.class);

        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        testStore = PopupStore.builder()
                .id(1L)
                .name("testStore")
                .build();

        testImage = Images.builder()
                .originName("test.jpg")
                .storedName("stored-test.jpg")
                .uploadUrl("https://test-url.com/test.jpg")
                .build();

        // image ID 설정
        ReflectionTestUtils.setField(testImage, "id", 1L);

        testReview = Review.builder()
                .content("Test Content")
                .rating(4.5)
                .user(testUser)
                .popupStore(testStore)
                .build();

        // review ID 설정
        ReflectionTestUtils.setField(testReview, "id", 1L);
        testReview.addImage(testImage);
    }

    @Nested
    @DisplayName("리뷰 생성 테스트")
    class CreateReview {
        private ReviewReqDto createDto;

        @BeforeEach
        void setUp() {
            MultipartFile mockFile = mock(MultipartFile.class);
            createDto = ReviewReqDto.builder()
                    .content("New Content")
                    .rating(4.5)
                    .images(List.of(mockFile))
                    .build();
        }

        @Test
        void 리뷰_생성_성공() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(popupStoreRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(reviewRepository.existsByUserAndPopupStore(testUser, testStore)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);
            given(imageService.uploadImageFromMultipart(any(), eq("Review"), any()))
                    .willReturn(testImage);

            // when
            ReviewRspDto result = reviewService.createReview(1L, createDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getImageUrls()).isNotEmpty();
            verify(reviewRepository).save(any(Review.class));
            verify(imageService).uploadImageFromMultipart(any(), eq("Review"), any());
        }

        @Test
        void 이미_존재하는_리뷰_생성_시도_실패() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(popupStoreRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(reviewRepository.existsByUserAndPopupStore(testUser, testStore)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, createDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 작성한 리뷰가 있습니다.");
        }
    }

    @Nested
    @DisplayName("리뷰 수정 테스트")
    class UpdateReview {
        private ReviewReqDto updateDto;

        @BeforeEach
        void setUp() {
            MultipartFile mockFile = mock(MultipartFile.class);
            updateDto = ReviewReqDto.builder()
                    .content("Updated Content")
                    .rating(4.0)
                    .images(List.of(mockFile))
                    .build();
        }

        @Test
        void 리뷰_수정_성공() {
            // given
            Images newImage = Images.builder()
                    .originName("new.jpg")
                    .storedName("stored-new.jpg")
                    .uploadUrl("https://test-url.com/new.jpg")
                    .build();

            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));
            given(imageService.uploadImageFromMultipart(any(), eq("Review"), any())).willReturn(newImage);
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);

            // when
            ReviewRspDto result = reviewService.updateReview(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            verify(reviewRepository).save(testReview);
            verify(imageService).uploadImageFromMultipart(any(), eq("Review"), any());
            verify(imageService).deleteImage(testImage.getId());
        }

        @Test
        void 권한_없는_사용자의_리뷰_수정_실패() {
            // given
            User unauthorizedUser = User.builder()
                    .id(2L)
                    .nickname("unauthorizedUser")
                    .build();

            given(loginUserProvider.getLoggedInUser()).willReturn(unauthorizedUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));

            // when & then
            assertThatThrownBy(() -> reviewService.updateReview(1L, updateDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리뷰 작성자만 삭제/수정이 가능합니다.");
        }
    }

    @Nested
    @DisplayName("리뷰 좋아요 테스트")
    class ToggleLike {
        @Test
        void 좋아요_추가_성공() throws InterruptedException {
            // given
            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
            given(mockLock.isHeldByCurrentThread()).willReturn(true);

            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewLikeRepository.existsByUserIdAndReviewId(1L, 1L)).willReturn(false);

            // when
            ReviewLikeRspDto result = reviewService.toggleLike(1L);

            // then
            assertThat(result.isLiked()).isTrue();
            verify(reviewLikeRepository).save(any(ReviewLike.class));
            verify(mockLock).unlock();
        }

        @Test
        void 좋아요_제거_성공() throws InterruptedException {
            // given
            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
            given(mockLock.isHeldByCurrentThread()).willReturn(true);

            ReviewLike reviewLike = ReviewLike.builder()
                    .id(1L)
                    .user(testUser)
                    .review(testReview)
                    .build();

            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewLikeRepository.existsByUserIdAndReviewId(1L, 1L)).willReturn(true);
            given(reviewLikeRepository.findByUserAndReview(testUser, testReview)).willReturn(Optional.of(reviewLike));

            // when
            ReviewLikeRspDto result = reviewService.toggleLike(1L);

            // then
            assertThat(result.isLiked()).isFalse();
            verify(reviewLikeRepository).delete(reviewLike);
            verify(mockLock).unlock();
        }

        @Test
        void 락_획득_실패() throws InterruptedException {
            // given
            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.toggleLike(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("잠시 후 다시 시도해 주세요.");
        }

        @Test
        void 예외_발생_시_unlock_호출() throws InterruptedException {
            // given
            given(redissonClient.getLock(anyString())).willReturn(mockLock);
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
            given(mockLock.isHeldByCurrentThread()).willReturn(true);

            given(reviewRepository.findById(1L)).willThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> reviewService.toggleLike(1L))
                    .isInstanceOf(BusinessException.class);

            // finally 블록에서 unlock 호출 확인
            verify(mockLock).unlock();
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 테스트")
    class DeleteReview {
        @Test
        void 리뷰_삭제_성공() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));

            // when
            reviewService.deleteReview(1L);

            // then
            verify(reviewRepository).delete(testReview);
            verify(imageService).deleteImage(testImage.getId());
        }

        @Test
        void 권한_없는_사용자의_리뷰_삭제_실패() {
            // given
            User unauthorizedUser = User.builder()
                    .id(2L)
                    .nickname("unauthorizedUser")
                    .build();

            given(loginUserProvider.getLoggedInUser()).willReturn(unauthorizedUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리뷰 작성자만 삭제/수정이 가능합니다.");
        }
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class GetReviews {
        private List<Review> sampleReviews;
        private PageRequest pageRequest;

        @BeforeEach
        void setUp() {
            pageRequest = PageRequest.of(0, 10);

            Review review1 = Review.builder()
                    .content("정말 좋은 팝업스토어였습니다.")
                    .rating(5.0)
                    .user(testUser)
                    .popupStore(testStore)
                    .build();

            Review review2 = Review.builder()
                    .content("괜찮은 경험이었습니다.")
                    .rating(3.0)
                    .user(testUser)
                    .popupStore(testStore)
                    .build();

            Review review3 = Review.builder()
                    .content("분위기가 좋았어요")
                    .rating(4.0)
                    .user(testUser)
                    .popupStore(testStore)
                    .build();

            // 각 리뷰에 이미지 추가
            Images image1 = Images.builder()
                    .originName("test1.jpg")
                    .storedName("stored-test1.jpg")
                    .uploadUrl("https://test-url.com/test1.jpg")
                    .build();
            review1.addImage(image1);

            Images image2 = Images.builder()
                    .originName("test2.jpg")
                    .storedName("stored-test2.jpg")
                    .uploadUrl("https://test-url.com/test2.jpg")
                    .build();
            review2.addImage(image2);

            Images image3 = Images.builder()
                    .originName("test3.jpg")
                    .storedName("stored-test3.jpg")
                    .uploadUrl("https://test-url.com/test3.jpg")
                    .build();
            review3.addImage(image3);

            sampleReviews = Arrays.asList(review1, review2, review3);
        }

        @Test
        void 최신순_조회_성공() {
            // given
            Page<Review> reviewPage = new PageImpl<>(sampleReviews);
            given(reviewRepository.findByPopupStoreIdOrderByCreatedAtDesc(1L, pageRequest)).willReturn(reviewPage);

            // when
            Page<ReviewRspDto> result = reviewService.getReviews(1L, ReviewSortType.RECENT, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(reviewRepository).findByPopupStoreIdOrderByCreatedAtDesc(1L, pageRequest);
        }

        @Test
        void 좋아요순_조회_성공() {
            // given
            Page<Review> reviewPage = new PageImpl<>(sampleReviews);
            given(reviewRepository.findByPopupStoreIdOrderByLikeCountDesc(1L, pageRequest)).willReturn(reviewPage);

            // when
            Page<ReviewRspDto> result = reviewService.getReviews(1L, ReviewSortType.LIKES, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(reviewRepository).findByPopupStoreIdOrderByLikeCountDesc(1L, pageRequest);
        }

        @Test
        void 높은평점순_조회_성공() {
            // given
            Page<Review> reviewPage = new PageImpl<>(sampleReviews);
            given(reviewRepository.findByPopupStoreIdOrderByRatingDesc(1L, pageRequest)).willReturn(reviewPage);

            // when
            Page<ReviewRspDto> result = reviewService.getReviews(1L, ReviewSortType.RATING_HIGH, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(reviewRepository).findByPopupStoreIdOrderByRatingDesc(1L, pageRequest);
        }

        @Test
        void 낮은평점순_조회_성공() {
            // given
            Page<Review> reviewPage = new PageImpl<>(sampleReviews);
            given(reviewRepository.findByPopupStoreIdOrderByRatingAsc(1L, pageRequest)).willReturn(reviewPage);

            // when
            Page<ReviewRspDto> result = reviewService.getReviews(1L, ReviewSortType.RATING_LOW, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(reviewRepository).findByPopupStoreIdOrderByRatingAsc(1L, pageRequest);
        }
    }
}
