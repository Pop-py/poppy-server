package com.poppy.domain.review.entity;

import com.poppy.common.entity.BaseTimeEntity;
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

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> reviewLikes = new ArrayList<>();

    @Builder
    private Review(Long id,String title, String content, String thumbnail, Double rating, User user, PopupStore popupStore) {
        this.title = title;
        this.content = content;
        this.thumbnail = thumbnail;
        this.rating = rating;
        this.user = user;
        this.popupStore = popupStore;
        this.reviewLikes = new ArrayList<>();
    }

    public void update(String title, String content, String thumbnail, Double rating) {
        this.title = title;
        this.content = content;
        this.thumbnail = thumbnail;
        this.rating = rating;
    }
}