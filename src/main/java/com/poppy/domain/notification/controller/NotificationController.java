package com.poppy.domain.notification.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.notification.dto.NotificationDto;
import com.poppy.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{id}")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public RspTemplate<List<NotificationDto>> getNotifications(@PathVariable Long id) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "알림 목록 조회 성공",
                notificationService.getNotifications(id)
        );
    }

    @PatchMapping("/notification/{notificationId}")
    public RspTemplate<?> markAsRead(@PathVariable Long id, @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return new RspTemplate<>(
                HttpStatus.OK,
                "알림 읽음 처리 성공"
        );
    }

    @DeleteMapping("/notification/{notificationId}")
    public RspTemplate<?> deleteNotification(@PathVariable Long id, @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return new RspTemplate<>(
                HttpStatus.OK,
                "알림 삭제 성공"
        );
    }
}