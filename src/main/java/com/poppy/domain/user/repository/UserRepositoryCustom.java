package com.poppy.domain.user.repository;

import com.poppy.domain.user.dto.response.UserPopupStoreRspDto;

import java.util.List;

public interface UserRepositoryCustom {
    List<UserPopupStoreRspDto> findRecentViewedStores(Long userId, int limit);
}
