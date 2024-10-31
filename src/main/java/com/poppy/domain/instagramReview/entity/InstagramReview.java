package com.poppy.domain.instagramReview.entity;

import com.poppy.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "instagram_reviews")
public class InstagramReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private BaseTimeEntity baseTime;

    // 추후 필드 추가
}
