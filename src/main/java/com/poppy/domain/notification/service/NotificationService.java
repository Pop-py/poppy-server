package com.poppy.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.poppy.common.config.redis.NotificationPublisher;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notification.dto.NotificationDto;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final NotificationMessageGenerator messageGenerator;
    private final NotificationPublisher notificationPublisher;
    private final LoginUserProvider loginUserProvider;

    @Transactional
    public void sendNotification(Waiting waiting, NotificationType type, Integer peopleAhead) {
        log.info("Sending notification.html to userId: {}", waiting.getUser());

        // FCM 알림 생성
        String fcmTitle = messageGenerator.generateFCMTitle(type, waiting.getPopupStore().getName());
        String fcmBody = messageGenerator.generateFCMBody(type, waiting.getWaitingNumber(), peopleAhead);

        NotificationDto fcmNotification = NotificationDto.from(
                waiting,
                fcmBody,
                type,
                peopleAhead,
                true
        );

        // FCM 알림 전송
        sendFCMNotification(waiting.getUser().getFcmToken(), fcmTitle, fcmNotification);

        // WebSocket 알림 생성
        String wsMessage = messageGenerator.generateWebSocketMessage(
                type,
                waiting.getPopupStore().getName(),
                waiting.getWaitingNumber(),
                peopleAhead);

        NotificationDto wsNotificationDto = NotificationDto.from(
                waiting,
                wsMessage,
                type,
                peopleAhead,
                false
        );

        // WebSocket 알림 DB 저장
        saveNotification(waiting, wsNotificationDto);

        // Redis로 WebSocket 알림 발행
        notificationPublisher.publish(wsNotificationDto);
    }

    // DB 저장
    private void saveNotification(Waiting waiting, NotificationDto dto) {
        notificationRepository.save(Notification.builder()
                .message(dto.getMessage())
                .type(dto.getType())
                .user(waiting.getUser())
                .popupStore(waiting.getPopupStore())
                .waitingNumber(dto.getWaitingNumber())
                .peopleAhead(dto.getPeopleAhead())
                .isFcm(false)
                .build());
    }

    // FCM 푸시 알림 전송
    private void sendFCMNotification(String fcmToken, String title, NotificationDto dto) {
        if (fcmToken == null) {
            log.warn("FCM token not found for user: {}", dto.getUserId());
            return;
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(dto.getMessage())
                        .build())
                .putData("type", dto.getType().name())
                .putData("storeId", dto.getStoreId().toString())
                .putData("waitingNumber", dto.getWaitingNumber().toString());

        // peopleAhead가 null이 아닐 때만 추가
        if (dto.getPeopleAhead() != null)
            messageBuilder.putData("peopleAhead", dto.getPeopleAhead().toString());

        Message message = messageBuilder.build();

        try {
            firebaseMessaging.send(message);
            log.info("FCM notification.html sent - type: {}, userId: {}",
                    dto.getType(), dto.getUserId());
        }
        catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification.html", e);
        }
    }

    // WebSocket 알림 최신순 30개 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(Long userId) {
        return notificationRepository.findTop30ByUserIdAndIsFcmFalseOrderByCreateTimeDesc(userId).stream()
                .map(NotificationDto::from)
                .collect(Collectors.toList());
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        validateNotificationOwner(notification);
        notification.markAsRead();
    }

    // 알림 삭제
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        validateNotificationOwner(notification);
        notificationRepository.delete(notification);
    }

    // 알림 소유자 검증
    private void validateNotificationOwner(Notification notification) {
        User loginUser = loginUserProvider.getLoggedInUser();
        if (!notification.getUser().getId().equals(loginUser.getId()))
            throw new BusinessException(ErrorCode.UNAUTHORIZED_NOTIFICATION_ACCESS);
    }
}
