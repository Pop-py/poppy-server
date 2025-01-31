package com.poppy.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public class ReservationNotificationDto extends NotificationDto {
    private Long popupStoreId;

    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate noticeDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime noticeTime;

    // 알림 생성 시 사용
    public static ReservationNotificationDto from(String message, NotificationType type, Long userId, Long popupStoreId, String popupStoreName, Boolean isRead) {
        return ReservationNotificationDto.builder()
                .message(message)
                .type(type)
                .userId(userId)
                .popupStoreId(popupStoreId)
                .popupStoreName(popupStoreName)
                .isRead(isRead)
                .noticeDate(LocalDate.now())
                .noticeTime(LocalTime.now())
                .build();
    }

    // 알림 조회 및 저장 시 사용
    public static ReservationNotificationDto from(Notification notification) {
        return ReservationNotificationDto.builder()
                .message(notification.getMessage())
                .type(notification.getType())
                .userId(notification.getUser().getId())
                .popupStoreId(notification.getPopupStore().getId())
                .popupStoreName(notification.getPopupStore().getName())
                .isRead(false)
                .noticeDate(LocalDate.now())
                .noticeTime(LocalTime.now())
                .build();
    }
}
