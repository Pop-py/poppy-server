package com.poppy.common.auth.controller;

import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestUserController {   // 테스트용 인증 코드 발급
    private final JwtTokenizer jwtTokenizer;
    private final OAuthOTUCache oAuthOTUCache;
    private final UserService userService;

    // 특정 유저 ID로 테스트용 액세스 토큰 발급
    @GetMapping("/token/{userId}")
    public String generateAccessTokenForUser(@PathVariable Long userId) {
        User user = userService.getById(userId); // 사용자 조회
        return jwtTokenizer.createAccessToken(user);
    }

    // 테스트용 일회성 인증 코드 발급
    @GetMapping("/verification-code/{userId}")
    public String generateTestVerificationCode(@PathVariable Long userId) {
        return oAuthOTUCache.putVerificationCodeInCache(userId);
    }

    // 테스트용으로 두 명의 랜덤 유저 생성
    @GetMapping("/users/generate-random")
    public ResponseEntity<String> createRandomMembers() {
        String randomData = UUID.randomUUID().toString().substring(0, 6);

        // 첫 번째 유저 생성
        User user1 = userService.login(randomData + "@example.com", randomData, "01012345678");

        // 두 번째 유저 생성
        User user2 = userService.login(randomData + "2@example.com", randomData + "2", "01012345678");

        return new ResponseEntity<>(user1.getEmail() + " 유저와 & " + user2.getEmail() + " 유저가 생성되었습니다.", HttpStatus.CREATED);
    }
}
