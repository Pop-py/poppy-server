package com.poppy.domain.notification.repository;

import com.poppy.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // FCM이 아닌(WebSocket) 알림을 최근 생성 시간 순으로 최대 30개 조회
    List<Notification> findTop30ByUserIdAndIsFcmFalseOrderByCreateTimeDesc(Long userId);

    // 유저별 알림 개수를 초과하는 알림만 조회
    @Query(value = """
    SELECT n.*
    FROM notifications n
    WHERE n.user_id = :userId
    AND n.id NOT IN (
        SELECT n2.id
        FROM notifications n2
        WHERE n2.user_id = :userId
        ORDER BY n2.create_time DESC
        LIMIT :limit
    )
""", nativeQuery = true)
    List<Notification> findNotificationsExceedingLimit(@Param("userId") Long userId, @Param("limit") int limit);
}