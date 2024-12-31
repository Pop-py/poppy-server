package com.poppy.common.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poppy.domain.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        try {
//            String channelName = new String(message.getChannel());
//            String messageBody = new String(message.getBody());
//
//            if ("notifications".equals(channelName)) {
//                NotificationDto notification = objectMapper.readValue(messageBody, NotificationDto.class);
//                sendWebSocketNotification(notification); // 웹소켓으로 전환
//            }
//        } catch (Exception e) {
//            log.error("Error processing Redis message: {}", e.getMessage(), e);
//        }
//    }
//
//    private void sendWebSocketNotification(NotificationDto notification) {
//        try {
//            String destination = String.format("/user/%s/queue/notifications", notification.getUserId());
//            messagingTemplate.convertAndSend(destination, notification);
//            log.info("WebSocket notification sent: {}", notification);
//        } catch (Exception e) {
//            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
//        }
//    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channelName = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            log.info("Received Redis message - Channel: {}", channelName);
            log.info("Message body: {}", messageBody);

            if ("notifications".equals(channelName)) {
                NotificationDto notification = objectMapper.readValue(messageBody, NotificationDto.class);
                log.info("Parsed notification - UserId: {}, Type: {}",
                        notification.getUserId(), notification.getType());
                sendWebSocketNotification(notification);
            }
        } catch (Exception e) {
            log.error("Error processing Redis message: {}", e.getMessage(), e);
        }
    }

    private void sendWebSocketNotification(NotificationDto notification) {
        try {
            String destination = String.format("/user/%s/queue/notifications", notification.getUserId());
            log.info("Sending WebSocket notification to: {}", destination);
            log.info("Sending notification object: {}", notification);
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Successfully sent WebSocket notification");
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
        }
    }
}
