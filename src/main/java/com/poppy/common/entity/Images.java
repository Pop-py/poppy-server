package com.poppy.common.entity;

import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.review.entity.Review;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor
public class Images extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_name", nullable = false)
    private String originName;  // 원본 파일 이름

    @Column(name = "stored_name", unique = true, nullable = false)
    private String storedName;  // S3에 저장되는 이름

    @Column(name = "upload_url", nullable = false)
    private String uploadUrl;  // S3 업로드 URL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_store_id")
    private PopupStore popupStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Builder
    public Images(String originName, String storedName, String uploadUrl) {
        this.originName = originName;
        this.storedName = storedName;
        this.uploadUrl = uploadUrl;
    }

    public void updatePopupStore(PopupStore popupStore) {
        this.popupStore = popupStore;
    }

    public void updateReview(Review review) {
        this.review = review;
    }
}
