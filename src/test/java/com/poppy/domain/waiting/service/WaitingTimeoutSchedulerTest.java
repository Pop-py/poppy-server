package com.poppy.domain.waiting.service;

import com.poppy.common.config.redis.DistributedLockService;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
class WaitingTimeoutSchedulerTest {
    @Autowired
    private WaitingTimeoutScheduler waitingScheduler;

    @MockBean
    private WaitingRepository waitingRepository;

    @MockBean
    private MasterWaitingService masterWaitingService;

    @MockBean
    private DistributedLockService lockService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private WaitingUtils waitingUtils;

    private Waiting waiting;
    private PopupStore popupStore;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        popupStore = PopupStore.builder()
                .id(1L)
                .name("테스트 팝업스토어")
                .build();

        waiting = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(1)
                .build();
        waiting.updateStatus(WaitingStatus.CALLED);

        ReflectionTestUtils.setField(waiting, "updateTime", LocalDateTime.now().minusMinutes(6));
        ReflectionTestUtils.setField(waiting, "id", 1L);
    }

    @Test
    void 락_획득_실패시_스케줄러_실행되지_않음() {
        // given
        when(lockService.tryLock(anyString())).thenReturn(false);

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(waitingRepository, never()).findByStatus(any());
        verify(masterWaitingService, never()).handleWaitingTimeout(any());
    }

    @Test
    void 호출된_웨이팅_5분_초과시_타임아웃_처리() {
        // given
        when(lockService.tryLock(anyString())).thenReturn(true);
        when(waitingRepository.findByStatus(WaitingStatus.CALLED))
                .thenReturn(List.of(waiting));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(masterWaitingService, times(1)).handleWaitingTimeout(waiting.getId());
    }

    @Test
    void 호출된_웨이팅_5분_이내면_타임아웃_처리되지_않음() {
        // given
        when(lockService.tryLock(anyString())).thenReturn(true);
        Waiting recentWaiting = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(2)
                .build();
        recentWaiting.updateStatus(WaitingStatus.CALLED);
        ReflectionTestUtils.setField(recentWaiting, "updateTime", LocalDateTime.now().minusMinutes(3));
        ReflectionTestUtils.setField(recentWaiting, "id", 2L);

        when(waitingRepository.findByStatus(WaitingStatus.CALLED))
                .thenReturn(List.of(recentWaiting));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(masterWaitingService, never()).handleWaitingTimeout(any());
    }

    @Test
    void 예외_발생시_락_정상_해제() {
        // given
        when(lockService.tryLock(anyString())).thenReturn(true);
        when(waitingRepository.findByStatus(any()))
                .thenThrow(new RuntimeException("테스트 예외"));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(lockService, times(1)).unlock(anyString());
    }

    @Test
    void 여러_웨이팅_동시_타임아웃_처리() {
        // given
        when(lockService.tryLock(anyString())).thenReturn(true);
        Waiting waiting2 = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(2)
                .build();
        waiting2.updateStatus(WaitingStatus.CALLED);
        ReflectionTestUtils.setField(waiting2, "updateTime", LocalDateTime.now().minusMinutes(7));
        ReflectionTestUtils.setField(waiting2, "id", 2L);

        when(waitingRepository.findByStatus(WaitingStatus.CALLED))
                .thenReturn(List.of(waiting, waiting2));

        // when
        waitingScheduler.checkWaitingTimeout();

        // then
        verify(masterWaitingService, times(1)).handleWaitingTimeout(waiting.getId());
        verify(masterWaitingService, times(1)).handleWaitingTimeout(waiting2.getId());
    }
}