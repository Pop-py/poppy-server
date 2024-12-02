package com.poppy.domain.review.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.likes.repository.ReviewLikeRepository;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
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
        @DisplayName("리뷰 생성 성공")
        void createReview_Success() {
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
        @DisplayName("이미 존재하는 리뷰 생성 시도 실패")
        void createReview_AlreadyExists() {
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
        @DisplayName("리뷰 수정 성공")
        void updateReview_Success() {
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
        @DisplayName("권한 없는 사용자의 리뷰 수정 실패")
        void updateReview_Unauthorized() {
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
        @DisplayName("좋아요 추가 성공")
        void toggleLike_Add() {
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
        @DisplayName("좋아요 제거 성공")
        void toggleLike_Remove() {
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
        @DisplayName("리뷰 삭제 성공")
        void deleteReview_Success() {
            // given
            given(loginUserProvider.getLoggedInUser()).willReturn(testUser);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(testReview));

            // when
            reviewService.deleteReview(1L);

            // then
            verify(reviewRepository).delete(testReview);
        }

        @Test
        @DisplayName("권한 없는 사용자의 리뷰 삭제 실패")
        void deleteReview_Unauthorized() {
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
}
