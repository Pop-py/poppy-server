package com.poppy.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.poppy.common.config.redis.NotificationPublisher;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notice.dto.NoticeRspDto;
import com.poppy.domain.notification.dto.*;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.user.repository.UserRepository;
import com.poppy.domain.waiting.entity.Waiting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
        if (waiting.getUser().getFcmToken() != null) {
            try {
                sendFCMNotificationWithNewTransaction(waiting.getUser().getFcmToken(), fcmTitle, fcmNotification);
            } catch (Exception e) {
                log.warn("Failed to send FCM notification for user {}: {}", waiting.getUser().getId(), e.getMessage());
            }
        }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendFCMNotificationWithNewTransaction(String token, String title, WaitingNotificationDto dto) {
        sendFCMNotification(token, title, dto);
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

    @Transactional
    public void send24HNotification(Reservation reservation) {
        log.info("Sending 24h before notification to userId: {}", reservation.getUser().getId());

        String storeName = reservation.getPopupStore().getName();

        // FCM 알림 생성 및 전송
        if (reservation.getUser().getFcmToken() != null) {
            String fcmTitle = messageGenerator.generateFCMTitle(NotificationType.REMIND_24H, storeName);
            String fcmBody = messageGenerator.generateFCMBody(NotificationType.REMIND_24H, null, null);

            Message message = Message.builder()
                    .setToken(reservation.getUser().getFcmToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(fcmTitle)
                            .setBody(fcmBody)
                            .build())
                    .putData("type", NotificationType.REMIND_24H.name())
                    .putData("storeId", reservation.getPopupStore().getId().toString())
                    .putData("reservationId", reservation.getId().toString())
                    .putData("reservationTime", reservation.getTime().toString())
                    .build();

            try {
                firebaseMessaging.send(message);
                log.info("FCM 24h before notification sent - userId: {}", reservation.getUser().getId());
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send FCM 24h before notification", e);
            }
        }

        // WebSocket 알림 생성
        String wsMessage = messageGenerator.generateWebSocketMessage(
                NotificationType.REMIND_24H,
                storeName,
                null,
                null
        );

        ReservationNotificationDto wsNotificationDto = ReservationNotificationDto.from(
                wsMessage,
                NotificationType.REMIND_24H,
                reservation.getUser().getId(),
                reservation.getPopupStore().getId(),
                storeName,
                false
        );

        // WebSocket 알림 DB 저장
        notificationRepository.save(Notification.builder()
                .message(wsMessage)
                .type(NotificationType.REMIND_24H)
                .user(reservation.getUser())
                .popupStore(reservation.getPopupStore())
                .isFcm(false)
                .build());

        // Redis로 WebSocket 알림 발행
        notificationPublisher.publish(wsNotificationDto);
    }

    @Transactional
    public void sendStoreOpeningNotification(Scrap scrap) {
        log.info("Sending store opening notification to userId: {}", scrap.getUser().getId());

        String storeName = scrap.getPopupStore().getName();

        // FCM 알림 전송
        if (scrap.getUser().getFcmToken() != null) {
            String fcmTitle = messageGenerator.generateFCMTitle(NotificationType.SCRAPED_STORE_OPENING, storeName);
            String fcmBody = messageGenerator.generateFCMBody(NotificationType.SCRAPED_STORE_OPENING, null, null);

            Message message = Message.builder()
                    .setToken(scrap.getUser().getFcmToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(fcmTitle)
                            .setBody(fcmBody)
                            .build())
                    .putData("type", NotificationType.SCRAPED_STORE_OPENING.name())
                    .putData("storeId", scrap.getPopupStore().getId().toString())
                    .build();

            try {
                firebaseMessaging.send(message);
                log.info("FCM store opening notification sent - userId: {}", scrap.getUser().getId());
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send FCM store opening notification", e);
            }
        }

        // WebSocket 알림 생성
        String wsMessage = messageGenerator.generateWebSocketMessage(
                NotificationType.SCRAPED_STORE_OPENING,
                storeName,
                null,
                null
        );

        ScrapedStoreNotificationDto wsNotificationDto = ScrapedStoreNotificationDto.of(
                wsMessage,
                NotificationType.SCRAPED_STORE_OPENING,
                scrap.getUser().getId(),
                scrap.getPopupStore().getId(),
                storeName,
                false
        );

        // WebSocket 알림 DB 저장
        notificationRepository.save(Notification.builder()
                .message(wsMessage)
                .type(NotificationType.SCRAPED_STORE_OPENING)
                .user(scrap.getUser())
                .popupStore(scrap.getPopupStore())
                .isFcm(false)
                .build());

        // Redis로 WebSocket 알림 발행
        notificationPublisher.publish(wsNotificationDto);
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
