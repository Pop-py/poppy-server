package com.poppy.domain.notification.dto;

import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class WaitingNotificationDto extends NotificationDto {
    private Long popupStoreId;
    private Integer waitingNumber;
    private Integer peopleAhead;

    // 알림 생성 시 사용
    public static WaitingNotificationDto of(Waiting waiting, String message, NotificationType type, Integer peopleAhead, boolean isFcm) {
        return WaitingNotificationDto.builder()
                .message(message)
                .type(type)
                .userId(waiting.getUser().getId())
                .popupStoreId(waiting.getPopupStore().getId())
                .popupStoreName(waiting.getPopupStore().getName())
                .waitingNumber(waiting.getWaitingNumber())
                .peopleAhead(peopleAhead)
                .isRead(!isFcm)
                .build();
    }

    // 알림 조회 시 사용
    public static WaitingNotificationDto from(Notification notification) {
        return WaitingNotificationDto.builder()
                .message(notification.getMessage())
                .type(notification.getType())
                .userId(notification.getUser().getId())
                .popupStoreId(notification.getPopupStore().getId())
                .popupStoreName(notification.getPopupStore().getName())
                .waitingNumber(notification.getWaitingNumber())
                .peopleAhead(notification.getPeopleAhead())
                .isRead(notification.isRead())
                .build();
    }
}