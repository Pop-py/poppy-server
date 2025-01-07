package com.poppy.domain.scrap.service;


import com.poppy.common.config.redis.DistributedLockService;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.scrap.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapOpeningNotificationScheduler {
    private static final String DAILY_SCHEDULE = "0 0 6 * * *";  // 매일 아침 6시

    private final ScrapRepository scrapRepository;
    private final NotificationService notificationService;
    private final DistributedLockService lockService;

    @Scheduled(cron = DAILY_SCHEDULE)
    @Transactional(readOnly = true)
    public void sendStoreOpeningNotifications() {
        if (!lockService.tryLock(DistributedLockService.SCRAP_STORE_OPENING_LOCK)) {
            log.debug("Failed to acquire scrap store opening notification lock. Skipping this execution.");
            return;
        }

        try {
            LocalDate today = LocalDate.now();

            // 오늘 오픈하는 스크랩된 팝업스토어 조회
            List<Scrap> scraps = scrapRepository.findByPopupStoreStartDate(today);

            for (Scrap scrap : scraps) {
                try {
                    notificationService.sendStoreOpeningNotification(scrap);
                    log.info("Sent store opening notification - storeId: {}, userId: {}, openingDate: {}",
                            scrap.getPopupStore().getId(),
                            scrap.getUser().getId(),
                            scrap.getPopupStore().getStartDate());
                } catch (Exception e) {
                    log.error("Failed to send store opening notification for scrap {}: {}",
                            scrap.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in scrap store opening notification scheduler: {}", e.getMessage(), e);
        } finally {
            lockService.unlock(DistributedLockService.SCRAP_STORE_OPENING_LOCK);
        }
    }
}