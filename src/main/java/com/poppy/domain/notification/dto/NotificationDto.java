package com.poppy.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.poppy.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WaitingNotificationDto.class, name = "WAITING"),
        @JsonSubTypes.Type(value = ReservationNotificationDto.class, name = "RESERVATION"),
        @JsonSubTypes.Type(value = NoticeNotificationDto.class, name = "NOTICE")
})
public abstract class NotificationDto {
    private String message;
    private NotificationType type;
    private Long userId;
    private String popupStoreName;
    private Boolean isRead;

    protected NotificationDto(String message, NotificationType type, Long userId, String popupStoreName, Boolean isRead) {
        this.message = message;
        this.type = type;
        this.userId = userId;
        this.popupStoreName = popupStoreName;
        this.isRead = isRead;
    }
}
