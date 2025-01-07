package com.poppy.domain.scrap.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    // 유저와 팝업스토어로 스크랩 존재 여부 확인
    boolean existsByUserAndPopupStore(User user, PopupStore popupStore);

    // 유저와 팝업스토어로 스크랩 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Scrap s WHERE s.user = :user AND s.popupStore = :popupStore")
    void deleteByUserAndPopupStore(@Param("user") User user, @Param("popupStore") PopupStore popupStore);

    // 유저의 스크랩 목록 조회 (+ 정렬 방식)
    @Query("""
       SELECT s
       FROM Scrap s
       JOIN s.popupStore p
       WHERE s.user = :user
       ORDER BY
       CASE WHEN :sortType = 'RECENT_SAVED' THEN s.createTime END DESC,
       CASE WHEN :sortType = 'OPEN_DATE' THEN p.startDate END ASC,
       CASE WHEN :sortType = 'END_DATE' THEN p.endDate END ASC
       """)
    List<Scrap> findScrapsByUserAndSortType(@Param("user") User user, @Param("sortType") String sortType);

    @Query("SELECT s FROM Scrap s " +
            "JOIN FETCH s.user u " +
            "JOIN FETCH s.popupStore p " +
            "WHERE p.startDate = :date")
    List<Scrap> findByPopupStoreStartDate(LocalDate date);
}