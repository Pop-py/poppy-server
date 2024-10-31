package com.poppy.domain.notification.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus type;  // 알림 유형 (예약, 대기 등)

    @Column(name = "is_read")
    private boolean isRead = false;  // 알림 목록 창에서 확인했는지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 팝업 스토어와 관련된 알림인 경우
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id")
    private PopupStore popupStore;

    @Embedded
    private BaseTimeEntity baseTime;
}
