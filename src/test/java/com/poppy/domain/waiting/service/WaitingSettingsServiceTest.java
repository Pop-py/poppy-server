package com.poppy.domain.waiting.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.dto.request.WaitingSettingsReqDto;
import com.poppy.domain.waiting.dto.response.WaitingSettingsRspDto;
import com.poppy.domain.waiting.entity.WaitingSettings;
import com.poppy.domain.waiting.repository.WaitingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingSettingsServiceTest {
    @Mock
    private WaitingSettingsRepository waitingSettingsRepository;
    @Mock
    private PopupStoreRepository popupStoreRepository;
    @Mock
    private LoginUserProvider loginUserProvider;

    @InjectMocks
    private WaitingSettingsService waitingSettingsService;

    private User masterUser;
    private PopupStore popupStore;
    private WaitingSettings waitingSettings;

    @BeforeEach
    void setUp() {
        masterUser = User.builder()
                .id(1L)
                .email("master@test.com")
                .role(Role.ROLE_MASTER)
                .build();

        popupStore = PopupStore.builder()
                .id(1L)
                .name("테스트 매장")
                .masterUser(masterUser)
                .build();

        waitingSettings = WaitingSettings.builder()
                .popupStore(popupStore)
                .maxWaitingCount(50)
                .build();
    }

    @Test
    void 대기설정_조회_성공() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingSettingsRepository.findByPopupStoreId(anyLong())).thenReturn(Optional.of(waitingSettings));

        // when
        WaitingSettingsRspDto result = waitingSettingsService.getSettings(1L);

        // then
        assertNotNull(result);
        assertEquals(50, result.getMaxWaitingCount());
    }

    @Test
    void 대기설정_수정_성공() {
        // given
        WaitingSettingsReqDto request = WaitingSettingsReqDto.builder()
                .maxWaitingCount(30)
                .build();

        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingSettingsRepository.findByPopupStoreId(anyLong())).thenReturn(Optional.of(waitingSettings));

        // when
        WaitingSettingsRspDto result = waitingSettingsService.updateSettings(1L, request);

        // then
        assertNotNull(result);
        assertEquals(30, result.getMaxWaitingCount());
    }

    @Test
    void 권한없는_사용자_접근_실패() {
        // given
        User unauthorizedUser = User.builder()
                .id(2L)
                .role(Role.ROLE_USER)
                .build();

        when(loginUserProvider.getLoggedInUser()).thenReturn(unauthorizedUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));

        // when & then
        assertThrows(BusinessException.class, () ->
                waitingSettingsService.getSettings(1L));
    }

    @Test
    void 기본설정_생성_성공() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingSettingsRepository.findByPopupStoreId(anyLong())).thenReturn(Optional.empty());
        when(waitingSettingsRepository.save(any())).thenReturn(waitingSettings);

        // when
        WaitingSettingsRspDto result = waitingSettingsService.getSettings(1L);

        // then
        assertNotNull(result);
        assertEquals(50, result.getMaxWaitingCount());
    }
}