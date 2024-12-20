package com.poppy.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.poppy.common.config.redis.NotificationPublisher;
import com.poppy.domain.notification.dto.NotificationDto;
import com.poppy.domain.notification.dto.ReservationNotificationDto;
import com.poppy.domain.notification.dto.WaitingNotificationDto;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.repository.NotificationRepository;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.reservation.entity.Reservation;
import com.poppy.domain.reservation.entity.ReservationStatus;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.entity.Waiting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FirebaseMessaging firebaseMessaging;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private NotificationMessageGenerator messageGenerator;
    @Mock
    private LoginUserProvider loginUserProvider;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private PopupStore popupStore;
    private Waiting waiting;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .fcmToken("test_token")
                .build();

        popupStore = PopupStore.builder()
                .id(1L)
                .name("테스트 매장")
                .masterUser(user)
                .build();

        waiting = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(1)
                .build();
    }

    @Test
    void FCM알림과_웹소켓알림_모두_발송() throws FirebaseMessagingException {
        // given
        String fcmTitle = "제목";
        String fcmBody = "FCM 내용";
        String wsMessage = "웹소켓 메시지";

        when(messageGenerator.generateFCMTitle(any(), any())).thenReturn(fcmTitle);
        when(messageGenerator.generateFCMBody(any(), any(), any())).thenReturn(fcmBody);
        when(messageGenerator.generateWebSocketMessage(any(), any(), any(), any()))
                .thenReturn(wsMessage);

        // when
        notificationService.sendNotification(waiting, NotificationType.WAITING_CALL, 5);

        // then
        verify(firebaseMessaging).send(any());
        verify(notificationPublisher).publish(any(WaitingNotificationDto.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void FCM토큰이_없으면_웹소켓알림만_발송() throws FirebaseMessagingException {
        // given
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .fcmToken(null)
                .build();

        waiting = Waiting.builder()
                .popupStore(popupStore)
                .user(user)
                .waitingNumber(1)
                .build();

        String wsMessage = "웹소켓 메시지";
        when(messageGenerator.generateWebSocketMessage(any(), any(), any(), any()))
                .thenReturn(wsMessage);

        // when
        notificationService.sendNotification(waiting, NotificationType.WAITING_CALL, 5);

        // then
        verify(firebaseMessaging, never()).send(any());
        verify(notificationPublisher).publish(any(WaitingNotificationDto.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void 알림_읽음_처리_성공() {
        // given
        Notification notification = Notification.builder()
                .user(user)
                .popupStore(popupStore)
                .message("테스트 메시지")
                .type(NotificationType.WAITING_CALL)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);

        // when
        notificationService.markAsRead(1L);

        // then
        assertTrue(notification.isRead());
    }

    @Test
    void 알림_삭제_성공() {
        // given
        Notification notification = Notification.builder()
                .user(user)
                .popupStore(popupStore)
                .message("테스트 메시지")
                .type(NotificationType.WAITING_CALL)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(loginUserProvider.getLoggedInUser()).thenReturn(user);

        // when
        notificationService.deleteNotification(1L);

        // then
        verify(notificationRepository).delete(notification);
    }
    @Test
    void 활동_알림_최신순_30개_조회_성공() {
        // given
        List<Notification> notifications = IntStream.range(0, 3)
                .mapToObj(i -> {
                    NotificationType type = i % 2 == 0 ?
                            NotificationType.WAITING_CALL : NotificationType.RESERVATION;
                    return Notification.builder()
                            .user(user)
                            .popupStore(popupStore)
                            .message("테스트 메시지 " + i)
                            .type(type)
                            .build();
                })
                .collect(Collectors.toList());

        when(notificationRepository.findTop30ByUserIdAndIsFcmFalseAndTypeNotOrderByCreateTimeDesc(
                anyLong(), eq(NotificationType.NOTICE)))
                .thenReturn(notifications);

        // when
        List<? extends NotificationDto> result = notificationService.getNotifications(1L);

        // then
        assertThat(result).hasSize(3);
        verify(notificationRepository)
                .findTop30ByUserIdAndIsFcmFalseAndTypeNotOrderByCreateTimeDesc(
                        eq(1L), eq(NotificationType.NOTICE));

        // 타입에 따른 DTO 변환 확인
        assertThat(result.get(0))
                .isInstanceOf(WaitingNotificationDto.class);
        assertThat(result.get(1))
                .isInstanceOf(ReservationNotificationDto.class);
        assertThat(result.get(2))
                .isInstanceOf(WaitingNotificationDto.class);
    }
}

