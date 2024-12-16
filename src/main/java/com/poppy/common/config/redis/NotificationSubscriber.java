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
                sendWebSocketNotification(notification);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }

    private void sendWebSocketNotification(NotificationDto notification) {
        try {
            String destination = String.format("/user/%s/queue/notifications", notification.getUserId());
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket notification.html sent: {}", notification);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification.html: {}", e.getMessage(), e);
        }
    }
}
