package com.poppy.domain.notification.service;

import com.poppy.common.config.redis.DistributedLockService;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {
    private static final String CLEANUP_SCHEDULE = "0 0 0 * * *"; // 매일 자정에 실행
    private static final int MAX_NOTIFICATIONS = 30; // 유저당 보관할 최대 알림 개수

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DistributedLockService lockService;

    @Scheduled(cron = CLEANUP_SCHEDULE)
    @Transactional
    public void cleanupOldNotifications() {
        // 10초 동안 락 획득 시도, 성공하면 5분 동안 락 유지
        if (!lockService.tryLock(DistributedLockService.NOTIFICATION_CLEANUP_LOCK, 10L, 300L)) {
            log.debug("Failed to acquire notification cleanup lock. Skipping this execution.");
            return;
        }

        try {
            List<User> users = userRepository.findAll();
            int deletedCount = 0;

            for (User user : users) {
                Long userId = user.getId();
                List<Notification> notificationsToDelete = notificationRepository.findNotificationsExceedingLimit(userId, MAX_NOTIFICATIONS);

                if (!notificationsToDelete.isEmpty()) {
                    notificationRepository.deleteAll(notificationsToDelete);
                    deletedCount += notificationsToDelete.size();
                }
            }

            log.info("Old notifications cleanup completed. Deleted {} notifications", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications: {}", e.getMessage(), e);
        } finally {
            lockService.unlock(DistributedLockService.NOTIFICATION_CLEANUP_LOCK);
        }
    }
}