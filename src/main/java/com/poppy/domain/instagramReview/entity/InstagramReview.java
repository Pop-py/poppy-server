package com.poppy.domain.instagramReview.entity;

import com.poppy.common.entity.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "instagram_reviews")
public class InstagramReview extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추후 필드 추가
}
