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
    private final PopularKeywordService popularKeywordService;
    private final LoginUserProvider loginUserProvider;

    // 이름으로 검색 후 검색어 저장 및 카운트 증가
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> searchStoresAndSaveHistory(String name) {
        popularKeywordService.incrementSearchCount(name);

        // 로그인한 경우에만 개인 검색 기록 저장
        try {
            loginUserProvider.getLoggedInUser();
            searchHistoryService.saveSearchHistory(name);
        }
        catch (BusinessException e) {
            if(e.getCode() != ErrorCode.UNAUTHORIZED.getCode()) throw e;
        }

        return popupStoreService.searchStoresByName(name);
    }
}
