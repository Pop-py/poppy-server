package com.poppy.domain.scrap.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    // 유저와 팝업스토어로 스크랩 존재 여부 확인
    boolean existsByUserAndPopupStore(User user, PopupStore popupStore);

    // 유저와 팝업스토어로 스크랩 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Scrap s WHERE s.user = :user AND s.popupStore = :popupStore")
    void deleteByUserAndPopupStore(@Param("user") User user, @Param("popupStore") PopupStore popupStore);

}