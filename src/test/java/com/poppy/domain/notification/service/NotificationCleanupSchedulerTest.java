package com.poppy.domain.notification.service;

import com.poppy.common.config.redis.DistributedLockService;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class NotificationCleanupSchedulerTest {
    @Autowired
    private NotificationCleanupScheduler notificationCleanupScheduler;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DistributedLockService lockService;

    private User user1;
    private User user2;
    private List<Notification> notificationsToDelete;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .email("test1@example.com")
                .build();

        user2 = User.builder()
                .id(2L)
                .email("test2@example.com")
                .build();

        notificationsToDelete = List.of(
                Notification.builder()
                        .message("테스트 알림 1")
                        .type(NotificationType.NOTICE)
                        .user(user1)
                        .build(),
                Notification.builder()
                        .message("테스트 알림 2")
                        .type(NotificationType.NOTICE)
                        .user(user1)
                        .build()
        );

        ReflectionTestUtils.setField(notificationsToDelete.get(0), "id", 1L);
        ReflectionTestUtils.setField(notificationsToDelete.get(1), "id", 2L);
    }

    @Test
    void 락_획득_실패시_스케줄러_실행되지_않음() {
        // given
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(false);

        // when
        notificationCleanupScheduler.cleanupOldNotifications();

        // then
        verify(userRepository, never()).findAll();
        verify(notificationRepository, never()).findNotificationsExceedingLimit(anyLong(), anyInt());
    }

    @Test
    void 삭제할_알림이_있는_경우_정상_삭제() {
        // given
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(notificationRepository.findNotificationsExceedingLimit(eq(user1.getId()), anyInt()))
                .thenReturn(notificationsToDelete);

        // when
        notificationCleanupScheduler.cleanupOldNotifications();

        // then
        verify(notificationRepository, times(1)).deleteAll(notificationsToDelete);
    }

    @Test
    void 삭제할_알림이_없는_경우_삭제_수행하지_않음() {
        // given
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(notificationRepository.findNotificationsExceedingLimit(eq(user1.getId()), anyInt()))
                .thenReturn(List.of());

        // when
        notificationCleanupScheduler.cleanupOldNotifications();

        // then
        verify(notificationRepository, never()).deleteAll(any());
    }

    @Test
    void 여러_유저의_알림_삭제_처리() {
        // given
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(notificationRepository.findNotificationsExceedingLimit(eq(user1.getId()), anyInt()))
                .thenReturn(notificationsToDelete);
        when(notificationRepository.findNotificationsExceedingLimit(eq(user2.getId()), anyInt()))
                .thenReturn(List.of());

        // when
        notificationCleanupScheduler.cleanupOldNotifications();

        // then
        verify(notificationRepository, times(1)).deleteAll(notificationsToDelete);
        verify(notificationRepository, times(2)).findNotificationsExceedingLimit(anyLong(), anyInt());
    }

    @Test
    void 예외_발생시_락_정상_해제() {
        // given
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userRepository.findAll())
                .thenThrow(new RuntimeException("테스트 예외"));

        // when
        notificationCleanupScheduler.cleanupOldNotifications();

        // then
        verify(lockService, times(1)).unlock(anyString());
    }
}