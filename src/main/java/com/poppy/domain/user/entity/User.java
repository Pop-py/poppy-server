package com.poppy.domain.user.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.common.entity.Token;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.wishList.entity.WishList;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
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

    @Enumerated(EnumType.STRING)
    private Role role;  // ROLE_USER, ROLE_ADMIN

    @Embedded
    private BaseTimeEntity baseTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WishList> wishLists = new ArrayList<>();

    // 회원가입용
    @Builder
    public User(String email, String phoneNumber, String nickname, String oauthProvider, Role role) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
        this.oauthProvider = oauthProvider;
        this.role = role;
        this.baseTime = new BaseTimeEntity(LocalDateTime.now(), LocalDateTime.now());
    }

    // 로그인용
    @Builder
    public User(String email, String nickname, String oauthProvider, Role role) {
        this.email = email;
        this.nickname = nickname;
        this.oauthProvider = oauthProvider;
        this.role = role;
    }

    // 위시 추가
//    public void addWish(PopupStore popupStore) {
//        WishList wishList = new WishList(this, popupStore);
//        wishLists.add(wishList);
//        popupStore.getWishLists().add(wishList);
//    }

    // 위시 삭제
    public void removeWish(PopupStore popupStore) {
        wishLists.removeIf(wish -> wish.getPopupStore().equals(popupStore));
    }
}
