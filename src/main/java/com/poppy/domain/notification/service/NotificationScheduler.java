package com.poppy.domain.notification.service;

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
public class NotificationScheduler {
    private static final int MAX_NOTIFICATIONS = 30;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            List<User> users = userRepository.findAll();
            int deletedCount = 0;

            for (User user : users) {
                Long userId = user.getId();

                // 삭제 대상 알림 조회
                List<Notification> notificationsToDelete = notificationRepository.findNotificationsExceedingLimit(userId, MAX_NOTIFICATIONS);

                if (!notificationsToDelete.isEmpty()) {
                    notificationRepository.deleteAll(notificationsToDelete);
                    deletedCount += notificationsToDelete.size();
                }
            }

            log.info("Old notifications cleanup completed. Deleted {} notifications", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications", e);
        }
    }
}

