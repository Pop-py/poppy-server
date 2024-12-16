package com.poppy.domain.waiting.service;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingSchedulerTest {
    @Mock
    private WaitingRepository waitingRepository;
    @Mock
    private MasterWaitingService masterWaitingService;

    @InjectMocks
    private WaitingScheduler waitingScheduler;

    private Waiting waiting;
    private User user;
    private PopupStore popupStore;

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
    void 호출상태_타임아웃_체크_성공() {
        // given
        waiting.updateStatus(WaitingStatus.CALLED);
        ReflectionTestUtils.setField(waiting, "updateTime", LocalDateTime.now().minusMinutes(6));  // 타임아웃 시간 초과
        when(waitingRepository.findByStatus(WaitingStatus.CALLED)).thenReturn(List.of(waiting));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(masterWaitingService).handleWaitingTimeout(waiting.getId());
    }

    @Test
    void 타임아웃_시간_미경과_처리안함() {
        // given
        waiting.updateStatus(WaitingStatus.CALLED);
        ReflectionTestUtils.setField(waiting, "updateTime", LocalDateTime.now().minusMinutes(4));  // 타임아웃 시간 미달
        when(waitingRepository.findByStatus(WaitingStatus.CALLED)).thenReturn(List.of(waiting));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(masterWaitingService, never()).handleWaitingTimeout(any());
    }
}