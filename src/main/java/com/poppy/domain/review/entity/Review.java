package com.poppy.domain.review.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.common.entity.Images;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "popup_store_id"})
)
public class Review extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "longtext")
    private String content;

    @Column(nullable = false)
    private Double rating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id", nullable = false)
    private PopupStore popupStore;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Images> images = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> reviewLikes = new ArrayList<>();

    @Builder
    private Review(String content, Double rating, User user, PopupStore popupStore) {
        this.content = content;
        this.rating = rating;
        this.user = user;
        this.popupStore = popupStore;
        this.images = new ArrayList<>();
        this.reviewLikes = new ArrayList<>();
    }

    public void updateReview(String content, Double rating) {
        this.content = content;
        this.rating = rating;
    }

    // 이미지 추가
    public void addImage(Images image) {
        this.images.add(image);
        image.updateReview(this);
    }
}