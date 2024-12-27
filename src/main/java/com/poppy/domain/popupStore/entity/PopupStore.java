package com.poppy.domain.popupStore.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.common.entity.Images;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.storeCategory.entity.StoreCategory;
import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "popup_stores")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupStore extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    private String location; // 위치 설명용

    @Column(nullable = false)
    private String address; // 실제 도로명 주소

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime; // 팝업스토어 운영 시작 시간

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime; // 팝업스토어 운영 종료 시간

    @Column(name = "available_slot", nullable = false)
    private Integer availableSlot;  // 예약 가능한 총 인원

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;  // 갑자기 사람 몰릴 때 대비

    @Column(name = "is_end", nullable = false)
    private Boolean isEnd;  // 팝업 스토어가 종료되었는지 확인

    @Column(nullable = false)
    private Double rating = 0.0;  // 5점 만점 (기본 0점, 리뷰 개수에 따라 점수 변동)

    @Column
    private Long price;

    @Column(name = "homepage_url")
    private String homepageUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "blog_url")
    private String blogUrl;

    @Column(nullable = false)
    private Integer scrapCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "reservation_type")
    private ReservationType reservationType;    // 팝업 스토어 예약 유형

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private StoreCategory storeCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="matser_user_id",nullable = false)
    private User masterUser;

    @OneToMany(mappedBy = "popupStore", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Images> images = new ArrayList<>();

    @OneToMany(mappedBy = "popupStore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationAvailableSlot> reservationAvailableSlots = new ArrayList<>();

    @OneToMany(mappedBy = "popupStore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scrap> scraps = new ArrayList<>();

    @OneToMany(mappedBy = "popupStore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PopupStoreView> views = new ArrayList<>();

    @Builder
    public PopupStore(String name, String description, String location, String address,
                       LocalDate startDate, LocalDate endDate, LocalTime openingTime,
                       LocalTime closingTime, Integer availableSlot, StoreCategory storeCategory,
                       Boolean isActive, Boolean isEnd, Double rating, Long price,
                       String homepageUrl, String instagramUrl, String blogUrl,
                       User masterUser, ReservationType reservationType, Integer scrapCount) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.availableSlot = availableSlot;
        this.storeCategory = storeCategory;
        this.isActive = isActive;
        this.isEnd = isEnd;
        this.rating = rating;
        this.price = price;
        this.homepageUrl = homepageUrl;
        this.instagramUrl = instagramUrl;
        this.blogUrl = blogUrl;
        this.masterUser = masterUser;
        this.reservationType = reservationType;
        this.scrapCount = scrapCount;
        this.images = new ArrayList<>();
        this.reservationAvailableSlots = new ArrayList<>();
        this.scraps = new ArrayList<>();
        this.views = new ArrayList<>();
    }

    public void updateScrapCount(Integer count) {
        this.scrapCount = count;
    }

    // 마감임박 여부 판단
    public Boolean calculateAlmostFull(List<ReservationAvailableSlot> slots, ReservationType reservationType) {
        // Offline인 경우 null
        if(reservationType == ReservationType.OFFLINE) return null;

        if(slots == null || slots.isEmpty()) return false;

        // 현재 시점 이후의 휴무일이 아닌 슬롯만 필터링
        List<ReservationAvailableSlot> activeSlots = slots.stream()
                .filter(slot -> !slot.getStatus().equals(PopupStoreStatus.HOLIDAY))
                .filter(slot -> {
                    LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getTime());
                    return !slotDateTime.isBefore(LocalDateTime.now());
                })
                .toList();

        if (activeSlots.isEmpty()) return false;

        // 남은 슬롯 수와 전체 슬롯 수 계산
        int remainingSlots = activeSlots.stream()
                .mapToInt(ReservationAvailableSlot::getAvailableSlot)
                .sum();

        int totalSlots = activeSlots.stream()
                .mapToInt(ReservationAvailableSlot::getTotalSlot)
                .sum();

        // 전체 슬롯의 20% 이하가 남은 경우 true
        return totalSlots > 0 && ((double) remainingSlots / totalSlots) <= 0.2;
    }
}
