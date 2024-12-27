package com.poppy.common.auth.controller;

import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
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
        String testNickname1 = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String testNickname2 = "test_" + UUID.randomUUID().toString().substring(0, 8);

        // 첫 번째 유저 생성 및 저장
        User user1 = userService.login(randomData + "@example.com", "01012345678");
        user1.updateNickname(testNickname1);
        userRepository.save(user1);

        // 두 번째 유저 생성 및 저장
        User user2 = userService.login(randomData + "2@example.com", "01012345678");
        user2.updateNickname(testNickname2);
        userRepository.save(user2);

        String message = String.format(
                "유저1(email: %s, nickname: %s) & 유저2(email: %s, nickname: %s)가 생성되었습니다.",
                user1.getEmail(), user1.getNickname(),
                user2.getEmail(), user2.getNickname()
        );

        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
}
