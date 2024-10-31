package com.poppy.common.entity;

import com.poppy.domain.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "token")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accessToken;

    @Column(nullable = false)
    private String refreshToken;

    @Embedded
    private BaseTimeEntity baseTime;

    // 같은 사용자라도 접속 방식이 다르면 Token이 다르기 때문에 ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}