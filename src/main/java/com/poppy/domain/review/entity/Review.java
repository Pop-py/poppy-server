package com.poppy.domain.review.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;

// 어플 자체 리뷰
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "popup_store_id"})
)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "longtext")
    private String content;

    private String thumbnail;

    @Column(nullable = false)
    private Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @Embedded
    private BaseTimeEntity baseTime;
}