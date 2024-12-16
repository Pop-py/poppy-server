package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStoreView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;


public interface PopupStoreViewRepository extends JpaRepository<PopupStoreView,Long> {

    // 현재 많이 찾는 팝업
    @Query("SELECT v.popupStore.id, COUNT(v.id) AS viewCount " +
            "FROM PopupStoreView v " +
            "WHERE v.viewedAt >= :startTime " +
            "GROUP BY v.popupStore.id " +
            "ORDER BY viewCount DESC")
    Page<Object[]> findPopularPopupStores(@Param("startTime") LocalDateTime startTime, Pageable pageable);

    // 현재 많이 찾는 (카테고리) 팝업
    @Query("SELECT v.popupStore.id, COUNT(v.id) AS viewCount " +
            "FROM PopupStoreView v " +
            "WHERE v.popupStore.storeCategory.id = :categoryId " +
            "AND v.viewedAt >= :startTime " +
            "GROUP BY v.popupStore.id " +
            "ORDER BY viewCount DESC")
    Page<Object[]> findPopularPopupStoresByCategory(
            @Param("categoryId") Long categoryId,
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);
}
