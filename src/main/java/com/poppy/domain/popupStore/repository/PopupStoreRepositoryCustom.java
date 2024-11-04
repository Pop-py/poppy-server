package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.entity.PopupStore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PopupStoreRepositoryCustom {
    List<PopupStore> findAllActive();
    List<PopupStore> findByCategory(String categoryName);
    List<PopupStore> findByLocation(String location);
    List<PopupStore> findByDateRange(LocalDate startDate, LocalDate endDate);
    List<PopupStore> findByKeyword(String name);
    List<PopupStore> findNewStores(LocalDateTime fromDate);
}
