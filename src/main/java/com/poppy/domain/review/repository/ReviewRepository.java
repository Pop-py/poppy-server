package com.poppy.domain.review.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.review.entity.Review;
import com.poppy.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {

    // 이미 review 를 작성한 사용자인지 확인
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r " +
            "WHERE r.user = :user AND r.popupStore = :popupStore")
    boolean existsByUserAndPopupStore(@Param("user") User user, @Param("popupStore") PopupStore popupStore);

    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.reviewLikes " +
            "WHERE r.id = :reviewId")
    Optional<Review> refresh(@Param("reviewId") Long reviewId);

    @Query("SELECT r FROM Review r WHERE r.popupStore.id = :popupStoreId ORDER BY r.createTime DESC")
    Page<Review> findByPopupStoreIdOrderByCreatedAtDesc(Long popupStoreId, Pageable pageable);

    @Query("""
       SELECT r FROM Review r
       WHERE r.popupStore.id = :popupStoreId
       ORDER BY SIZE(r.reviewLikes) desc
       """)
    Page<Review> findByPopupStoreIdOrderByLikeCountDesc(Long popupStoreId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.popupStore.id = :popupStoreId ORDER BY r.rating DESC")
    Page<Review> findByPopupStoreIdOrderByRatingDesc(Long popupStoreId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.popupStore.id = :popupStoreId ORDER BY r.rating ASC")
    Page<Review> findByPopupStoreIdOrderByRatingAsc(Long popupStoreId, Pageable pageable);


    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.popupStore.id = :popupStoreId")
    Double calculateAverageRatingByPopupStore(@Param("popupStoreId") Long popupStoreId);

    List<Review> findByUserIdOrderByCreateTimeDesc(Long userId);
}
