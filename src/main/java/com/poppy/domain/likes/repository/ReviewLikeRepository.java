package com.poppy.domain.likes.repository;

import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike,Long> {

    @Query("SELECT CASE WHEN COUNT(rl) > 0 THEN true ELSE false END " +
            "FROM ReviewLike rl " +
            "WHERE rl.user.id = :userId " +
            "AND rl.review.id = :reviewId")
    boolean existsByUserIdAndReviewId(@Param("userId") Long userId, @Param("reviewId") Long reviewId);

    @Query("SELECT rl FROM ReviewLike rl " +
            "WHERE rl.user = :user " +
            "AND rl.review = :review")
    Optional<ReviewLike> findByUserAndReview(@Param("user") User user, @Param("review") Review review);

}
