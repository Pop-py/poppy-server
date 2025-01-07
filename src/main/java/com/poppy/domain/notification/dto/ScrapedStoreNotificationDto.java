package com.poppy.domain.notification.dto;

import com.poppy.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class ScrapedStoreNotificationDto extends NotificationDto {
    private Long storeId;

    private ScrapedStoreNotificationDto(String message, NotificationType type, Long userId, Long storeId, String popupStoreName, Boolean isRead) {
        super(message, type, userId, popupStoreName, isRead);
        this.storeId = storeId;
    }

    public static ScrapedStoreNotificationDto of(
            String message,
            NotificationType type,
            Long userId,
            Long storeId,
            String popupStoreName,
            Boolean isRead
    ) {
        return new ScrapedStoreNotificationDto(message, type, userId, storeId, popupStoreName, isRead);
    }
}