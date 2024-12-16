package com.poppy.domain.waiting.service;

import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.repository.WaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingUtilsTest {
    @Mock
    private WaitingRepository waitingRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WaitingUtils waitingUtils;

    private PopupStore popupStore;
    private User user;
    private Waiting waiting;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .role(Role.ROLE_USER)
                .build();

        popupStore = PopupStore.builder()
                .id(1L)
                .name("테스트 매장")
                .masterUser(user)
                .build();

        waiting = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(1)
                .build();
    }

    @Test
    void 대기_순서_업데이트_성공() {
        // given
        List<Waiting> waitingList = List.of(waiting);
        when(waitingRepository.findById(anyLong())).thenReturn(Optional.of(waiting));
        when(waitingRepository.findByPopupStoreIdAndStatusAndWaitingNumberGreaterThanOrderByWaitingNumberAsc(
                anyLong(), any(), anyInt())).thenReturn(waitingList);
        when(waitingRepository.countPeopleAhead(anyLong(), anyInt(), anySet())).thenReturn(0);

        // when
        waitingUtils.updateWaitingQueue(1L, 1L);

        // then
        verify(notificationService).sendNotification(waiting, NotificationType.TEAMS_AHEAD, 0);
    }
}