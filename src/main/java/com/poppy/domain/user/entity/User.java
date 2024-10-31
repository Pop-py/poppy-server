package com.poppy.domain.user.entity;

import com.poppy.common.entity.BaseTimeEntity;
import com.poppy.common.entity.Token;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.wishList.entity.WishList;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

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
