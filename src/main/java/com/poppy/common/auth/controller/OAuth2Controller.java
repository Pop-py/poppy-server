package com.poppy.common.auth.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.common.auth.dto.TokenRspDto;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {
    private final OAuthOTUCache oAuthOTUCache;
    private final JwtTokenizer jwtTokenizer;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/token")
    public RspTemplate<TokenRspDto> token(String code) {
        try {
            long userId = oAuthOTUCache.getUserId(code);
            User user = userService.getById(userId);

            TokenRspDto tokenRspDto = jwtTokenizer.generateTokens(user);
            return new RspTemplate<>(HttpStatus.OK, "토큰 발급", tokenRspDto);
        }
        catch (NumberFormatException e) {
            return new RspTemplate<>(HttpStatus.BAD_REQUEST, "토큰 발급 실패");
        }
    }

    // 리프레시 토큰을 통해 액세스 토큰 발급
    @PostMapping("/refresh")
    public RspTemplate<TokenRspDto> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        // 토큰 검증
        try {
            jwtTokenizer.parseRefreshToken(refreshToken);
        }
        catch (MalformedJwtException e) {
            return new RspTemplate<>(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Claims claims = jwtTokenizer.parseRefreshToken(refreshToken);   // 토큰 파싱
        Long userId = claims.get("userId", Long.class);
        String redisKey = "user:" + userId;
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        // Redis에 저장된 리프레시 토큰 검증
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            return new RspTemplate<>(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        // User 조회 및 AccessToken 재발급
        User user = userService.getById(userId);
        String newAccessToken = jwtTokenizer.createAccessToken(user);

        TokenRspDto tokenRspDto = TokenRspDto.builder()
                .accessToken(newAccessToken)
                .accessTokenExp(jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpireTime()))
                .refreshToken(refreshToken)
                .refreshTokenExp(jwtTokenizer.getTokenExpiration(jwtTokenizer.getRefreshTokenExpireTime()))
                .userEmail(user.getEmail())
                .build();

        return new RspTemplate<>(HttpStatus.OK, "AccessToken 재발급 완료", tokenRspDto);
    }
}
