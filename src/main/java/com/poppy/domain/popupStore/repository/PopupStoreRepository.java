package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PopupStoreRepository extends JpaRepository<PopupStore, Long>, PopupStoreRepositoryCustom {
    Optional<PopupStore> findById(Long id);
}