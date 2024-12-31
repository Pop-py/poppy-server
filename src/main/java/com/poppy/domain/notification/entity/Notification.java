package com.poppy.domain.notification.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
public class Notification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;  // 알림 유형

    @Column(name = "is_read")
    private boolean isRead = false;  // 알림 목록 창에서 확인했는지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 팝업 스토어와 관련된 알림인 경우
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id")
    private PopupStore popupStore;

    @Column(name = "waiting_number")
    private Integer waitingNumber;

    @Column(name = "people_ahead")
    private Integer peopleAhead;

    @Column(name = "is_fcm")
    private boolean isFcm;

    @Builder
    public Notification(String message, NotificationType type, User user,
                        PopupStore popupStore, Integer waitingNumber, Integer peopleAhead, boolean isFcm) {
        this.message = message;
        this.type = type;
        this.user = user;
        this.popupStore = popupStore;
        this.waitingNumber = waitingNumber;
        this.peopleAhead = peopleAhead;
        this.isFcm = isFcm;
    }

    public Notification(String message, NotificationType type, User user, PopupStore popupStore) {
        this.message = message;
        this.type = type;
        this.user = user;
        this.popupStore = popupStore;
        this.isFcm = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
