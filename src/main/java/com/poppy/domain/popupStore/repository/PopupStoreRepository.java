package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopupStoreRepository extends JpaRepository<PopupStore, Long>, PopupStoreRepositoryCustom {
}