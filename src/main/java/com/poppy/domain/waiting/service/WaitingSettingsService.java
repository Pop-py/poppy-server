package com.poppy.domain.waiting.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.dto.request.WaitingSettingsReqDto;
import com.poppy.domain.waiting.dto.response.WaitingSettingsRspDto;
import com.poppy.domain.waiting.entity.WaitingSettings;
import com.poppy.domain.waiting.repository.WaitingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitingSettingsService {
    private final WaitingSettingsRepository waitingSettingsRepository;
    private final PopupStoreRepository popupStoreRepository;
    private final LoginUserProvider loginUserProvider;

    private void validateMasterAuthority(Long storeId) {
        User master = loginUserProvider.getLoggedInUser();
        PopupStore store = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (!store.getMasterUser().getId().equals(master.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_STORE_ACCESS);
        }
    }

    // 대기 설정 조회
    @Transactional
    public WaitingSettingsRspDto getSettings(Long storeId) {
        validateMasterAuthority(storeId);
        WaitingSettings settings = waitingSettingsRepository.findByPopupStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));

        return WaitingSettingsRspDto.from(settings);
    }

    // 대기 설정 수정
    @Transactional
    public WaitingSettingsRspDto updateSettings(Long storeId, WaitingSettingsReqDto waitingSettingsReqDto) {
        validateMasterAuthority(storeId);
        WaitingSettings settings = waitingSettingsRepository.findByPopupStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));

        settings.updateMaxCount(waitingSettingsReqDto.getMaxWaitingCount());

        return WaitingSettingsRspDto.from(settings);
    }

    // 기본 설정 생성
    private WaitingSettings createDefaultSettings(Long storeId) {
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        WaitingSettings settings = WaitingSettings.builder()
                .popupStore(popupStore)
                .maxWaitingCount(50)
                .build();

        return waitingSettingsRepository.save(settings);
    }
}
