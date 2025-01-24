package com.poppy.common.auth;

import com.poppy.common.auth.dto.TokenRspDto;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@Getter
@Slf4j
public class JwtTokenizer {
    @Value("${jwt.secret-key}")
    private String secretKey;   // secret key

    @Value("${jwt.access-token-expire-time}")
    private int accessTokenExpireTime;     // access token 만료 시간

    @Value("${jwt.refresh-token-expire-time}")
    private int refreshTokenExpireTime;    // refresh token 만료 시간

    public TokenRspDto generateTokens(User user) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return TokenRspDto.builder()
                .accessToken(accessToken)
                .accessTokenExp(getTokenExpiration(accessTokenExpireTime))
                .refreshToken(refreshToken)
                .refreshTokenExp(getTokenExpiration(refreshTokenExpireTime))
                .userId(user.getId())
                .userEmail(user.getEmail())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    // JWT 토큰 생성
    private String createToken(Map<String, Object> claims, String audience, int expire) {
        return Jwts.builder()
                .setClaims(claims)
                .setAudience(audience)
                .setIssuedAt(new Date())
                .setExpiration(getTokenExpiration(expire))
                .signWith(createHmacShaKeyFromSecretKey(secretKey))
                .compact();
    }

    // access token 발급
    public String createAccessToken(User user) {
        Claims claims = Jwts.claims().setSubject(user.getEmail());
        claims.put("userId", user.getId());
        claims.put("username", user.getEmail());
        claims.put("role", user.getRole().name());  // Role을 문자열로 저장

        log.info("access token 발급");
        System.out.println(createToken(claims, claims.getId(), accessTokenExpireTime));
        return createToken(claims, claims.getId(), accessTokenExpireTime);
    }

    // refresh token 발급
    public String createRefreshToken(User user) {
        Claims claims = Jwts.claims().setSubject(user.getEmail());
        claims.put("userId", user.getId());
        claims.put("username", user.getEmail());
        claims.put("role", user.getRole().name());

        log.info("refresh token 발급");
        System.out.println(createToken(claims, claims.getId(), refreshTokenExpireTime));
        return createToken(claims, claims.getId(), refreshTokenExpireTime);
    }

    // 토큰 파싱 및 검증
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(createHmacShaKeyFromSecretKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims parseAccessToken(String accessToken) {
        return parseToken(accessToken);
    }

    public Claims parseRefreshToken(String refreshToken) {
        return parseToken(refreshToken);
    }

    // 만료 시간 계산 (밀리초 단위)
    public Date getTokenExpiration(int expirationMillis) {
        return new Date(System.currentTimeMillis() + expirationMillis);
    }

    // 서명 키 생성
    private static Key createHmacShaKeyFromSecretKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 사용하지 않으므로 일단 주석 처리 (추후 삭제 또는 이용)
//    public Jws<Claims> getClaims(String jws, String secretKey) {
//        Key key = createHmacShaKeyFromSecretKey(secretKey);
//
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(jws);
//    }

    // claims에서 권한 목록 가져오기
    public List<GrantedAuthority> getAuthoritiesFromClaims(Claims claims) {
        String roleName = claims.get("role", String.class);
        Role role = Role.valueOf(roleName);
        return List.of((GrantedAuthority) role::name);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseAccessToken(token);
        return claims.get("userId", Long.class);  // createAccessToken에서 넣었던 userId를 추출
    }
}
