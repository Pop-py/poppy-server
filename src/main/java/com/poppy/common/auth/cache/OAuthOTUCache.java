package com.poppy.common.auth.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class OAuthOTUCache {    // 일회성 인증 코드 관리
    // Cache<인증 코드, user_id>
    private final Cache<String, Long> codeExpirationCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES) // 캐시 생성 후 30초 뒤 만료
            .build();

    // 검증 코드 생성 및 저장
    public String putVerificationCodeInCache(long userId) {
        String verificationCode = UUID.randomUUID().toString();     // UUID로 검증 코드 생성
        codeExpirationCache.put(verificationCode, userId);

        return verificationCode;
    }

    // user 조회
    public long getUserId(String verificationCode) {
        // 검증 코드가 없는 경우 예외 처리
        if (!StringUtils.hasText(verificationCode)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        Long memberId = codeExpirationCache.getIfPresent(verificationCode);
        codeExpirationCache.invalidate(verificationCode);   // 일회성을 보장하기 위하여 유저를 찾으면 검증 코드 제거

        // 유저 id가 null인 경우 예외
        return Objects.requireNonNull(memberId, "유저를 찾을 수 없습니다.");
    }
}