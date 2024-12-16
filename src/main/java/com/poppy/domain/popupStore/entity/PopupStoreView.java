package com.poppy.domain.popupStore.entity;

import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popup_store_views")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupStoreView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt; // 조회 시각
}
