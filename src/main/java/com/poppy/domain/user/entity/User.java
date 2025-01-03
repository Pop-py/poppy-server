package com.poppy.domain.user.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.notification.entity.Notification;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.waiting.entity.Waiting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String nickname;

    @Column(nullable = false)
    private String oauthProvider;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scrap> scraps = new ArrayList<>();

    @OneToMany(mappedBy = "masterUser",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PopupStore> masterPopupStore = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> likedReviews = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Waiting> waitings = new ArrayList<>();

    public User(Long id) {
        this.id = id;
    }

    @Builder
    public User(String email, String phoneNumber, String nickname, String oauthProvider, Role role) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
        this.oauthProvider = oauthProvider;
        this.role = role;
    }

    // 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void upgradeToMaster() {
        this.role = Role.ROLE_MASTER;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
