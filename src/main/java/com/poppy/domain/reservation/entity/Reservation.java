package com.poppy.domain.reservation.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@NoArgsConstructor
@Getter
public class Reservation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;  // 예약한 팝업 스토어 날짜

    @Column(nullable = false)
    private LocalDateTime time;  // 예약한 팝업 스토어 시간

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;  // STAND_BY, CHECKED, CANCELED (예약 상태: 대기/확인/취소)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @Builder
    public Reservation(PopupStore popupStore, User user, ReservationStatus status, LocalDateTime time, LocalDate date) {
        this.popupStore = popupStore;
        this.user = user;
        this.status = status;
        this.time = time;
        this.date = date;
    }
}
