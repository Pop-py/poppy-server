package com.poppy.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.poppy.common.config.redis.NotificationPublisher;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notice.dto.NoticeRspDto;
import com.poppy.domain.notification.dto.NoticeNotificationDto;
import com.poppy.domain.notification.dto.NotificationDto;
import com.poppy.domain.notification.dto.ReservationNotificationDto;
import com.poppy.domain.notification.dto.WaitingNotificationDto;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.user.repository.UserRepository;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final UserRepository userRepository;

    // 웨이팅 알림 전송
    @Transactional
    public void sendNotification(Waiting waiting, NotificationType type, Integer peopleAhead) {
        log.info("Sending notification.html to userId: {}", waiting.getUser());

        // FCM 알림 생성
        String fcmTitle = messageGenerator.generateFCMTitle(type, waiting.getPopupStore().getName());
        String fcmBody = messageGenerator.generateFCMBody(type, waiting.getWaitingNumber(), peopleAhead);

        WaitingNotificationDto fcmNotification = WaitingNotificationDto.of(
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

        // wsMessage가 null이면 알림을 발송하지 않음
        if (wsMessage == null) return;

        WaitingNotificationDto wsWaitingNotificationDto = WaitingNotificationDto.of(
                waiting,
                wsMessage,
                type,
                peopleAhead,
                false
        );

        // WebSocket 알림 DB 저장
        saveNotification(waiting, wsWaitingNotificationDto);

        // Redis로 WebSocket 알림 발행
        notificationPublisher.publish(wsWaitingNotificationDto);
    }

    // 웨이팅 DB 저장
    private void saveNotification(Waiting waiting, WaitingNotificationDto dto) {
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

    // 예약 알림 전송
    @Transactional
    public void sendNotification(Reservation reservation, ReservationStatus status, NotificationType type) {
        // WebSocket 알림 생성
        String wsMessage = messageGenerator.generateWebSocketMessage(
                status,
                reservation.getPopupStore().getName(),
                reservation.getDate().toString(),
                reservation.getTime().toString(),
                reservation.getPerson()
        );

        // wsMessage가 null이면 알림을 발송하지 않음
        if (wsMessage == null) return;

        ReservationNotificationDto reservationNotificationDto = ReservationNotificationDto.from(
                wsMessage,
                type,
                reservation.getUser().getId(),
                reservation.getPopupStore().getId(),
                reservation.getPopupStore().getName(),
                false
        );

        // WebSocket 알림 DB 저장
        saveNotification(reservation, reservationNotificationDto, type);

        // Redis로 WebSocket 알림 발행
        notificationPublisher.publish(reservationNotificationDto);
    }

    // 예약 알림 DB 저장
    private void saveNotification(Reservation reservation, ReservationNotificationDto dto, NotificationType type) {
        notificationRepository.save(Notification.builder()
                .message(dto.getMessage())
                .type(type)
                .user(reservation.getUser())
                .popupStore(reservation.getPopupStore())
                .build());
    }

    // 공지사항 알림 전송
    @Transactional
    public void sendNotice(NoticeRspDto noticeRspDto) {
        List<User> users = userRepository.findByRole(Role.ROLE_USER);

        String title = noticeRspDto.getTitle();

        // notice 제목에서 [카테고리]와 제목 분리
        String categoryPattern = "\\[(.*?)\\]";
        String subjectPattern = "\\](.+)";
        Pattern categoryRegex = Pattern.compile(categoryPattern);
        Pattern subjectRegex = Pattern.compile(subjectPattern);
        Matcher categoryMatcher = categoryRegex.matcher(title);
        Matcher subjectMatcher = subjectRegex.matcher(title);

        String noticeCategory = "";
        String noticeSubject = "";
        if (categoryMatcher.find()) {
            noticeCategory = categoryMatcher.group(1);
        }
        if (subjectMatcher.find()) {
            noticeSubject = subjectMatcher.group(1).trim();
        }

        for (User user : users) {
            // WebSocket 메시지 생성
            String wsMessage = messageGenerator.generateWebSocketMessage(noticeCategory, noticeSubject);

            NoticeNotificationDto notificationDto = NoticeNotificationDto.of(user, wsMessage);

            // WebSocket 알림 DB 저장
            saveNotification(user, notificationDto);

            // Redis로 WebSocket 알림 발행
            notificationPublisher.publish(notificationDto);

            // FCM 알림 전송
            if (user.getFcmToken() != null) {
                sendNoticeFCM(user.getFcmToken(), String.format("[%s]", noticeCategory), noticeSubject);
            }
        }
    }

    // 공지사항 알림 DB 저장
    private void saveNotification(User user, NoticeNotificationDto dto) {
        notificationRepository.save(Notification.builder()
                .message(dto.getMessage())
                .type(NotificationType.NOTICE)
                .user(user)
                .isFcm(false)
                .build());
    }

    // FCM 푸시 알림 전송
    private void sendFCMNotification(String fcmToken, String title, WaitingNotificationDto dto) {
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
                .putData("storeId", dto.getPopupStoreId().toString())
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

    private void sendNoticeFCM(String fcmToken, String title, String content) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(content)
                        .build())
                .putData("type", NotificationType.NOTICE.name())
                .build();

        try {
            firebaseMessaging.send(message);
            log.info("FCM notice sent to token: {}", fcmToken);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notice", e);
        }
    }

    // 활동 알림 최신순 30개 목록 조회
    @Transactional(readOnly = true)
    public List<? extends NotificationDto> getNotifications(Long userId) {
        return notificationRepository.findTop30ByUserIdAndIsFcmFalseAndTypeNotOrderByCreateTimeDesc(
                        userId,
                        NotificationType.NOTICE
                )
                .stream()
                .map(notification -> {
                    if(notification.getType() == NotificationType.RESERVATION_CHECK || notification.getType() == NotificationType.RESERVATION_CANCEL)
                        return ReservationNotificationDto.from(notification);
                    else return WaitingNotificationDto.from(notification);
                })
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
