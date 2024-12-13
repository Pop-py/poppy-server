package com.poppy.domain.notification.service;

import com.poppy.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationContent {
    private String message;
    private NotificationType type;
}
