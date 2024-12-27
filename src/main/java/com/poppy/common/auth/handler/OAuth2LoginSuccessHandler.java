package com.poppy.common.auth.handler;

import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserService userService;
    private final OAuthOTUCache oauthOTUCache;
    private final JwtTokenizer jwtTokenizer;
    private final RedisTemplate<String, String> redisTemplate;

    public OAuth2LoginSuccessHandler(UserService userService, OAuthOTUCache oauthOTUCache, JwtTokenizer jwtTokenizer, RedisTemplate<String, String> redisTemplate) {
        this.userService = userService;
        this.oauthOTUCache = oauthOTUCache;
        this.jwtTokenizer = jwtTokenizer;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User naverUser = (OAuth2User) authentication.getPrincipal();  // 로그인한 유저 정보

        Map<String, String> responseMap = (Map<String, String>) naverUser.getAttributes().get("response");

        String email = responseMap.get("email");
        String phoneNumber = responseMap.get("mobile").replace("-", "");

        User user = userService.login(email, phoneNumber);     // 가입되어 있으면 로그인, 아닌 경우 회원가입
        redirect(request, response, user);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        log.info("Naver Login Success!");
        String verificationCode = oauthOTUCache.putVerificationCodeInCache(user.getId());

        // 닉네임이 없는 경우 닉네임 설정 페이지로 리다이렉트
        if (user.getNickname() == null) {
            String uri = createURI("/signup", verificationCode).toString();
            getRedirectStrategy().sendRedirect(request, response, uri);
            return;
        }

        // 기존 사용자의 경우 JWT Access Token 및 Refresh Token 생성
        String accessToken = jwtTokenizer.createAccessToken(user);
        String refreshToken = jwtTokenizer.createRefreshToken(user);

        // 응답 헤더에 Token 추가
        response.addHeader("Authorization", "Bearer " + accessToken);

        // Redis에 RefreshToken 저장 (user:{id} 형식)
        String redisKey = "user:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, jwtTokenizer.getRefreshTokenExpireTime(), TimeUnit.MINUTES);

        String uri = createURI("/token", verificationCode).toString();

        getRedirectStrategy().sendRedirect(request, response, uri);
    }

    // OAuth2 로그인 성공 시 토큰값과 함께 반환될 URL 설정
    private URI createURI(String path, String verificationCode) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("code", verificationCode);

        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
//                .host("poppy-fe-git-pop-36-feature-1-notice-poppy-ca4d5978.vercel.app")
//                .host("pop-py.duckdns.org")
                .host("localhost")
                .port(3000)
                .path(path)
                .queryParams(queryParams)
                .build()
                .toUri();
    }
}
