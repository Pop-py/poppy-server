package com.poppy.common.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poppy.domain.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channelName = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            if ("notifications".equals(channelName)) {
                NotificationDto notification = objectMapper.readValue(messageBody, NotificationDto.class);
                sendWebSocketNotification(notification); // 웹소켓으로 전환
            }
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }

    private void sendWebSocketNotification(NotificationDto notification) {
        try {
            // 특정 사용자에게 알림 전송
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(notification.getUserId()), // userId를 username으로 사용
                    destination,
                    notification
            );
            log.info("개인 알림 전송 - message: {}, type: {}, userId: {}, popupStoreName: {}, isRead: {}",
                    notification.getMessage(),
                    notification.getType(),
                    notification.getUserId(),
                    notification.getPopupStoreName(),
                    notification.getIsRead());

            // 모든 사용자에게 공지사항 전송 (공지사항인 경우)
            if (notification.getType().name().equals("NOTICE")) {
                String globalDestination = "/topic/notifications";
                messagingTemplate.convertAndSend(globalDestination, notification);
                log.info("공지사항 전송 - message: {}", notification.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
        }
    }
}
