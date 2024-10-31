package com.poppy.domain.wishList.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "wish_lists")
@Getter
public class WishList {  // User와 PopupStore의 연결 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @Embedded
    private BaseTimeEntity baseTime;
}
