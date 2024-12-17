package com.poppy.domain.notification.dto;

import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class ReservationNotificationDto extends NotificationDto {
    // 알림 생성 시 사용
    public static ReservationNotificationDto from(String message, NotificationType type, Long userId, String popupStoreName, Boolean isRead) {
        return ReservationNotificationDto.builder()
                .message(message)
                .type(type)
                .userId(userId)
                .popupStoreName(popupStoreName)
                .isRead(isRead)
                .build();
    }

    // 알림 조회 및 저장 시 사용
    public static ReservationNotificationDto from(Notification notification) {
        return ReservationNotificationDto.builder()
                .message(notification.getMessage())
                .type(notification.getType())
                .userId(notification.getUser().getId())
                .popupStoreName(notification.getPopupStore().getName())
                .isRead(false)
                .build();
    }
}
