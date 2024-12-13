package com.poppy.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationDto {
    private String message;
    private NotificationType type;
    private Long userId;
    private Long storeId;
    private String popupStoreName;
    private Integer waitingNumber;
    private Integer peopleAhead;
    private Boolean isRead;

    public static NotificationDto from(
            Waiting waiting,
            String message,
            NotificationType type,
            Integer peopleAhead,
            boolean isFcm) {
        return NotificationDto.builder()
                .message(message)
                .type(type)
                .userId(waiting.getUser().getId())
                .storeId(waiting.getPopupStore().getId())
                .popupStoreName(waiting.getPopupStore().getName())
                .waitingNumber(waiting.getWaitingNumber())
                .peopleAhead(peopleAhead)
                .isRead(!isFcm)
                .build();
    }

    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .message(notification.getMessage())
                .type(notification.getType())
                .userId(notification.getUser().getId())
                .storeId(notification.getPopupStore().getId())
                .popupStoreName(notification.getPopupStore().getName())
                .waitingNumber(notification.getWaitingNumber())
                .peopleAhead(notification.getPeopleAhead())
                .isRead(notification.isRead())
                .build();
    }
}