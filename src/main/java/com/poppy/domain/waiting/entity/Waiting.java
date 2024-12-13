package com.poppy.domain.waiting.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Waiting extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer waitingNumber;  // 대기 번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitingStatus status = WaitingStatus.WAITING;

    @Builder
    public Waiting(PopupStore popupStore, User user, Integer waitingNumber) {
        this.popupStore = popupStore;
        this.user = user;
        this.waitingNumber = waitingNumber;
    }

    public void updateStatus(WaitingStatus status) {
        this.status = status;
    }
}
