package com.poppy.domain.search.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.user.repository.LoginUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreSearchService {
    private final PopupStoreService popupStoreService;
    private final SearchHistoryService searchHistoryService;
    private final LoginUserProvider loginUserProvider;

    // 이름으로 검색 후 검색어 저장
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> searchStoresAndSaveHistory(String name) {
        // 로그인한 경우 검색어 저장
        try {
            loginUserProvider.getLoggedInUser();
            searchHistoryService.saveSearchHistory(name);
        } catch (BusinessException e) {
            // 로그인 하지 않은 경우는 예외를 던지지 않음
            if(e.getCode() != ErrorCode.UNAUTHORIZED.getCode()) throw e;
        }

        return popupStoreService.searchStoresByName(name);
    }
}
