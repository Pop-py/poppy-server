package com.poppy.common.auth.handler;

import com.poppy.common.auth.JwtTokenizer;
import com.poppy.common.auth.cache.OAuthOTUCache;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserService userService;
    private final OAuthOTUCache oauthOTUCache;
    private final JwtTokenizer jwtTokenizer;

    public OAuth2LoginSuccessHandler(UserService userService, OAuthOTUCache oauthOTUCache, JwtTokenizer jwtTokenizer) {
        this.userService = userService;
        this.oauthOTUCache = oauthOTUCache;
        this.jwtTokenizer = jwtTokenizer;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User naverUser = (OAuth2User) authentication.getPrincipal();  // 로그인한 유저 정보

        Map<String, String> responseMap = (Map<String, String>) naverUser.getAttributes().get("response");

        String email = responseMap.get("email");
        String nickname = responseMap.get("nickname");

        User user = userService.login(email, nickname);     // 가입되어 있으면 로그인, 아닌 경우 회원가입
        redirect(request, response, user);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        log.info("Naver Login Success!");
        // JWT Access Token 및 Refresh Token 생성
        String accessToken = jwtTokenizer.createAccessToken(user);
        String refreshToken = jwtTokenizer.createRefreshToken(user);

        // 응답 헤더에 Token 추가
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("Refresh-Token", refreshToken);

        String verificationCode = oauthOTUCache.putVerificationCodeInCache(user.getId());
        String uri = createURI(verificationCode).toString();

        getRedirectStrategy().sendRedirect(request, response, uri);
    }

    // OAuth2 로그인 성공 시 토큰값과 함께 반환될 URL 설정
    private URI createURI(String verificationCode) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("code", verificationCode);

        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
//                .host("pop-py.duckdns.org")
                .host("localhost")
                .port(8080)
                .path("/token")
                .queryParams(queryParams)
                .build()
                .toUri();
    }
}
