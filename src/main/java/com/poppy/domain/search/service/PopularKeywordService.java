package com.poppy.domain.search.service;

import com.poppy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularKeywordService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CURRENT_POPULAR_KEY = "popular:keywords:current";  // 현재 시간대 고정 순위
    private static final String NEXT_POPULAR_KEY = "popular:keywords:next";        // 다음 시간대 집계중
    private static final int TOP_SIZE = 10;

    // 검색어 카운트 증가 (다음 시간대 집계용)
    public void incrementSearchCount(String keyword) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                try {
                    operations.multi();
                    // 다음 시간대 키워드 카운트 증가
                    operations.opsForZSet().incrementScore(NEXT_POPULAR_KEY, keyword, 1);
                    return operations.exec();
                } catch (Exception e) {
                    operations.discard();
                    throw new BusinessException("인기 검색어 갱신에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        });
    }

    // 현재 시간대의 Top 10 인기 검색어 조회 (고정된 순위)
    public List<String> getTopKeywords() {
        Set<String> topKeywords = redisTemplate.opsForZSet()
                .reverseRange(CURRENT_POPULAR_KEY, 0, TOP_SIZE - 1);
        return topKeywords != null ? new ArrayList<>(topKeywords) : new ArrayList<>();
    }

    // 매 정각마다 호출되어 순위 변경
    public void switchHourlyKeywords() {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                try {
                    operations.multi();
                    // 현재 시간대 키 삭제
                    operations.delete(CURRENT_POPULAR_KEY);
                    // 다음 시간대 키를 현재 시간대로 복사 (next는 그대로 유지)
                    operations.copy(NEXT_POPULAR_KEY, CURRENT_POPULAR_KEY, true);
                    return operations.exec();
                } catch (Exception e) {
                    operations.discard();
                    throw new BusinessException("인기 검색어 갱신에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        });
    }
}
