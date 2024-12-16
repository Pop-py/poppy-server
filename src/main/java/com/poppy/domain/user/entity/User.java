package com.poppy.domain.user.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.domain.likes.entity.ReviewLike;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.wishList.entity.WishList;
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

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String oauthProvider;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishList> wishLists = new ArrayList<>();

    @OneToMany(mappedBy = "masterUser",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PopupStore> masterPopupStore = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewLike> likedReviews = new ArrayList<>();

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

    // 로그인 시 네이버 정보를 바탕으로 업데이트
    public void updateLoginInfo(String nickname, String oauthProvider, Role role) {
        this.nickname = nickname;
        this.oauthProvider = oauthProvider;
        this.role = role;
    }

    // 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void upgradeToMaster(){
        this.role = Role.ROLE_MASTER;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    // 위시 추가
//    public void addWish(PopupStore popupStore) {
//        WishList wishList = new WishList(this, popupStore);
//        wishLists.add(wishList);
//        popupStore.getWishLists().add(wishList);
//    }

    // 위시 삭제
//    public void removeWish(PopupStore popupStore) {
//        wishLists.removeIf(wish -> wish.getPopupStore().equals(popupStore));
//    }
}
