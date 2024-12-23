package com.poppy.domain.popupStore.repository;

import com.poppy.domain.popupStore.dto.request.PopupStoreSearchReqDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import java.time.LocalDateTime;
import java.util.List;

public interface PopupStoreRepositoryCustom {
    List<PopupStore> findAllActive();
    List<PopupStore> findByKeyword(String name);
    List<PopupStore> findNewStores(LocalDateTime fromDate);
    List<PopupStore> findBySearchCondition(PopupStoreSearchReqDto popupStoreSearchReqDto);
}
