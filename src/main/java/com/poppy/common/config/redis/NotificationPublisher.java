package com.poppy.common.config.redis;

import com.poppy.domain.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {
    private final RedisTemplate<String, Object> notificationRedisTemplate;
    private static final String NOTIFICATION_TOPIC = RedisConfig.NOTIFICATION_TOPIC;

    public <T extends NotificationDto> void publish(T notification) {
        try {
            notificationRedisTemplate.convertAndSend(NOTIFICATION_TOPIC, notification);
        } catch (Exception e) {
            log.error("Failed to publish notification: {}", e.getMessage(), e);
        }
    }
}