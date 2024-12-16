package com.poppy.domain.waiting.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingSettings extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @Column(nullable = false)
    private Integer maxWaitingCount = 50;  // 최대 대기 인원 (기본값 50명)
    @Builder
    public WaitingSettings(PopupStore popupStore, Integer maxWaitingCount) {
        this.popupStore = popupStore;
        this.maxWaitingCount = maxWaitingCount;
    }

    public void updateMaxCount(Integer maxWaitingCount) {
        this.maxWaitingCount = maxWaitingCount;
    }
}
