package com.poppy.domain.waiting.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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

    @Column(name = "waiting_date", nullable = false)
    private LocalDate waitingDate;    // 대기 예약 날짜

    @Column(name = "waiting_time", nullable = false)
    private LocalTime waitingTime;    // 대기 예약 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitingStatus status = WaitingStatus.WAITING;

    @Builder
    public Waiting(PopupStore popupStore, User user, Integer waitingNumber, LocalDate waitingDate, LocalTime waitingTime) {
        this.popupStore = popupStore;
        this.user = user;
        this.waitingNumber = waitingNumber;
        this.waitingDate = LocalDate.now();
        this.waitingTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);;
    }

    public void updateStatus(WaitingStatus status) {
        this.status = status;
    }
}
