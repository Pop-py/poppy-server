package com.poppy.domain.review.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.likes.repository.ReviewLikeRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    private User testUser;
    private PopupStore testStore;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // 공통적으로 사용하는 객체 초기화
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        testStore = PopupStore.builder()
                .id(1L)
                .name("testStore")
                .build();

        testReview = Review.builder()
                .id(1L)
                .title("Test Review")
                .content("Test Content")
                .thumbnail("thumbnail.jpg")
                .rating(4.5)
                .user(testUser)
                .popupStore(testStore)
                .build();
    }

    @Nested
    @DisplayName("리뷰 생성 테스트")
    class CreateReview {

        private ReviewReqDto createDto;

        @BeforeEach
        void setUp() {
            createDto = ReviewReqDto.builder()
                    .title("New Review")
                    .content("New Content")
                    .thumbnail("new-thumbnail.jpg")
                    .rating(4.5)
                    .build();
        }

        @Test
        void 리뷰_생성_성공() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(popupStoreRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(reviewRepository.existsByUserAndPopupStore(testUser, testStore)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(testReview);

            // when
            ReviewRspDto result = reviewService.createReview(1L, createDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(testReview.getTitle());
            verify(reviewRepository).save(any(Review.class));
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
            updateDto = ReviewReqDto.builder()
                    .title("Updated Review")
                    .content("Updated Content")
                    .thumbnail("updated-thumbnail.jpg")
                    .rating(4.0)
                    .build();
        }

        @Test
        void 리뷰_수정_성공() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));

            // when
            ReviewRspDto result = reviewService.updateReview(1L, updateDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(updateDto.getTitle());
            verify(reviewRepository).save(testReview);
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
        void 좋아요_추가_성공() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewLikeRepository.existsByUserIdAndReviewId(1L, 1L)).willReturn(false);

            // when
            ReviewLikeRspDto result = reviewService.toggleLike(1L);

            // then
            assertThat(result.isLiked()).isTrue();
            verify(reviewLikeRepository).save(any(ReviewLike.class));
        }

        @Test
        void 좋아요_제거_성공() {
            // given
            ReviewLike reviewLike = ReviewLike.builder()
                    .id(1L)
                    .user(testUser)
                    .review(testReview)
                    .build();

            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewLikeRepository.existsByUserIdAndReviewId(1L, 1L)).willReturn(true);
            given(reviewLikeRepository.findByUserAndReview(testUser, testReview))
                    .willReturn(Optional.of(reviewLike));

            // when
            ReviewLikeRspDto result = reviewService.toggleLike(1L);

            // then
            assertThat(result.isLiked()).isFalse();
            verify(reviewLikeRepository).delete(reviewLike);
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
    class GetReviews {

        private List<Review> sampleReviews;
        private PageRequest pageRequest;

        @BeforeEach
        void setUp() {
            pageRequest = PageRequest.of(0, 10);

            sampleReviews = Arrays.asList(
                    Review.builder()
                            .id(1L)
                            .title("Great Store!")
                            .content("정말 좋은 팝업스토어였습니다.")
                            .thumbnail("thumbnail1.jpg")
                            .rating(5.0)
                            .user(testUser)
                            .popupStore(testStore)
                            .build(),
                    Review.builder()
                            .id(2L)
                            .title("Good Experience")
                            .content("괜찮은 경험이었습니다.")
                            .thumbnail("thumbnail2.jpg")
                            .rating(3.0)
                            .user(testUser)
                            .popupStore(testStore)
                            .build(),
                    Review.builder()
                            .id(3L)
                            .title("Nice Place")
                            .content("분위기가 좋았어요")
                            .thumbnail("thumbnail3.jpg")
                            .rating(4.0)
                            .user(testUser)
                            .popupStore(testStore)
                            .build()
            );
        }

        @Test
        void 최신순_조회_성공() {
            // given
            Page<Review> reviewPage = new PageImpl<>(sampleReviews);
            given(reviewRepository.findByPopupStoreIdOrderByCreatedAtDesc(1L, pageRequest))
                    .willReturn(reviewPage);

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
            given(reviewRepository.findByPopupStoreIdOrderByLikeCountDesc(1L, pageRequest))
                    .willReturn(reviewPage);

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
            given(reviewRepository.findByPopupStoreIdOrderByRatingDesc(1L, pageRequest))
                    .willReturn(reviewPage);

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
            given(reviewRepository.findByPopupStoreIdOrderByRatingAsc(1L, pageRequest))
                    .willReturn(reviewPage);

            // when
            Page<ReviewRspDto> result = reviewService.getReviews(1L, ReviewSortType.RATING_LOW, pageRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(reviewRepository).findByPopupStoreIdOrderByRatingAsc(1L, pageRequest);
        }
    }
}
