package com.poppy.domain.waiting.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.dto.response.DailyWaitingRspDto;
import com.poppy.domain.waiting.dto.response.WaitingRspDto;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterWaitingServiceTest {
    @Mock
    private WaitingRepository waitingRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WaitingUtils waitingUtils;
    @Mock
    private PopupStoreRepository popupStoreRepository;
    @Mock
    private LoginUserProvider loginUserProvider;

    @InjectMocks
    private MasterWaitingService masterWaitingService;

    private User masterUser;
    private PopupStore popupStore;
    private Waiting waiting;

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

        waiting = Waiting.builder()
                .popupStore(popupStore)
                .user(User.builder().id(2L).build())
                .waitingNumber(1)
                .waitingDate(LocalDate.now())
                .waitingTime(LocalTime.now())
                .build();
    }

    @Test
    void 날짜별_대기목록_조회_성공() {
        // given
        LocalDate date = LocalDate.now();
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingRepository.findWaitingsByStoreIdAndDate(anyLong(), any(LocalDate.class))).thenReturn(List.of(waiting));

        // when
        List<DailyWaitingRspDto> result = masterWaitingService.getDailyWaitings(1L, date);

        // then
        assertFalse(result.isEmpty());
    }

    @Test
    void 시간대별_대기목록_조회_성공() {
        // given
        LocalDate date = LocalDate.now();
        int hour = 14;  // 오후 2시

        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingRepository.findWaitingsByStoreIdAndDateTime(anyLong(), any(LocalDate.class), anyInt()))
                .thenReturn(List.of(waiting));

        // when
        List<DailyWaitingRspDto> result = masterWaitingService.getHourlyWaitings(1L, date, hour);

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void 대기_상태_업데이트_성공() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingRepository.findById(anyLong())).thenReturn(Optional.of(waiting));

        // when
        List<WaitingRspDto> result = masterWaitingService.updateWaitingStatus(1L, 1L, WaitingStatus.CALLED);

        // then
        assertNotNull(result);
        verify(notificationService).sendNotification(waiting, NotificationType.WAITING_CALL, null);
    }

    @Test
    void 대기_상태_업데이트_권한없음_실패() {
        // given
        User unauthorizedUser = User.builder().id(3L).role(Role.ROLE_USER).build();
        when(loginUserProvider.getLoggedInUser()).thenReturn(unauthorizedUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));

        // when & then
        assertThrows(BusinessException.class, () ->
                masterWaitingService.updateWaitingStatus(1L, 1L, WaitingStatus.CALLED));
    }

    @Test
    void 활성화된_대기목록_조회_성공() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(waitingRepository.findActiveWaitings(anyLong(), any())).thenReturn(List.of(waiting));

        // when
        List<WaitingRspDto> result = masterWaitingService.getActiveWaitings(1L);

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void 대기_타임아웃_처리_성공() {
        // given
        waiting.updateStatus(WaitingStatus.CALLED);
        when(waitingRepository.findById(anyLong())).thenReturn(Optional.of(waiting));
        when(popupStoreRepository.findById(anyLong())).thenReturn(Optional.of(popupStore));
        when(loginUserProvider.getLoggedInUser()).thenReturn(masterUser);

        // when
        masterWaitingService.handleWaitingTimeout(1L);

        // then
        assertEquals(WaitingStatus.CANCELED, waiting.getStatus());
    }
}
