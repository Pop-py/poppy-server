package com.poppy.common.auth.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.common.auth.dto.TokenRspDto;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {
    private final OAuthOTUCache oAuthOTUCache;
    private final JwtTokenizer jwtTokenizer;
    private final UserService userService;

    @GetMapping("/token")
    public RspTemplate<?> token(String code) {
        try {
            long userId = oAuthOTUCache.getUserId(code);
            User user = userService.getById(userId);

            TokenRspDto tokenRspDto = jwtTokenizer.generateTokens(user);
            return new RspTemplate<>(HttpStatus.OK, "토큰 발급", tokenRspDto);
        }
        catch (Exception e) {
            return new RspTemplate<>(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 발급 실패", null);
        }
    }
}
