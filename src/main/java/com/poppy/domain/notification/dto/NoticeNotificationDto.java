package com.poppy.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@SuperBuilder
@NoArgsConstructor
public class NoticeNotificationDto extends NotificationDto {
    private String title;

    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate noticeDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime noticeTime;

    protected NoticeNotificationDto(String message, NotificationType type, Long userId,
                                    String popupStoreName, Boolean isRead,
                                    String title, LocalDate noticeDate, LocalTime noticeTime) {
        super(message, type, userId, popupStoreName, isRead);
        this.title = title;
        this.noticeDate = noticeDate;
        this.noticeTime = noticeTime;
    }

    public static NoticeNotificationDto of(User user, String message) {
        return NoticeNotificationDto.builder()
                .message(message)
                .type(NotificationType.NOTICE)
                .userId(user.getId())
                .popupStoreName(null)
                .isRead(false)
                .noticeDate(LocalDate.now())
                .noticeTime(LocalTime.now())
                .build();
    }
}