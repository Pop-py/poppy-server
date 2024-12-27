package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PopupStoreRepository extends JpaRepository<PopupStore, Long>, PopupStoreRepositoryCustom {
    Optional<PopupStore> findById(Long id);

    @Query("SELECT p FROM PopupStore p WHERE p.id IN :ids")
    List<PopupStore> findAllById(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM PopupStore p " +
            "WHERE p.startDate > :today " +
            "ORDER BY p.startDate ASC")
    List<PopupStore> findAllFuturePopupStores(@Param("today") LocalDate today);

    @Query("SELECT p FROM PopupStore p WHERE p.address LIKE %:address%")
    List<PopupStore> findByAddress(@Param("address") String address);
}