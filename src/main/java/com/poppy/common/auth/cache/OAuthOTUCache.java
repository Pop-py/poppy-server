package com.poppy.common.auth.cache;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuthOTUCache {    // 일회성 인증 코드 관리
    private final RedisTemplate<String, String> redisTemplate;

    // 검증 코드 생성 및 저장
    public String putVerificationCodeInCache(long userId) {
        String verificationCode = UUID.randomUUID().toString();     // UUID로 검증 코드 생성
        redisTemplate.opsForValue().set(verificationCode, String.valueOf(userId), 10, TimeUnit.MINUTES);

        return verificationCode;
    }

    // user 조회
    public long getUserId(String verificationCode) {
        // 검증 코드가 없는 경우 예외 처리
        if (!StringUtils.hasText(verificationCode)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        String memberId = redisTemplate.opsForValue().get(verificationCode);

        // 유저 id가 null인 경우 예외
        return Objects.requireNonNull(Long.valueOf(memberId), "유저를 찾을 수 없습니다.");
    }
}