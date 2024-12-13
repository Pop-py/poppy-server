package com.poppy.domain.search.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryService {
    private final LoginUserProvider loginUserProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "search:history:";
    private static final int MAX_HISTORY_SIZE = 10;     // 최대 저장 개수

    public void saveSearchHistory(String keyword) {
        User loggedInUser = loginUserProvider.getLoggedInUser();
        String key = generateKey(loggedInUser.getId());

        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                try {
                    operations.multi();  // 트랜잭션 시작

                    ZSetOperations<String, String> zSetOps = operations.opsForZSet();
                    double score = System.currentTimeMillis();

                    zSetOps.add(key, keyword, score);

                    // 크기 체크 후 초과분 제거
                    Long size = zSetOps.size(key);
                    if(size != null && size >= MAX_HISTORY_SIZE)
                        zSetOps.removeRange(key, 0, size - MAX_HISTORY_SIZE);

                    // 만료 시간 설정
                    operations.expire(key, 7, TimeUnit.DAYS);

                    // 트랜잭션 커밋
                    List<Object> result = operations.exec();
                    if(result.isEmpty()) throw new BusinessException("검색어 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

                    return result;
                }
                catch (Exception e) {
                    operations.discard();
                    throw new BusinessException("검색어 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
                }
            }
        });
    }

    // 검색 기록 조회
    public List<String> getSearchHistory() {
        User loggedInUser = loginUserProvider.getLoggedInUser();
        String key = generateKey(loggedInUser.getId());

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Set<String> history = zSetOps.reverseRange(key, 0, -1);     // 최근 시간부터 정렬
        return history != null ? new ArrayList<>(history) : new ArrayList<>();
    }

    // 특정 검색 기록 삭제
    public boolean deleteSearchKeyword(String keyword) {
        User loggedInUser = loginUserProvider.getLoggedInUser();
        String key = generateKey(loggedInUser.getId());

        try {
            // 트랜잭션으로 삭제 작업 수행
            return (Boolean.TRUE.equals(redisTemplate.execute(new SessionCallback<>() {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    try {
                        operations.multi();
                        operations.opsForZSet().remove(key, keyword);
                        List<Object> result = operations.exec();

                        if (result.isEmpty()) return false;

                        // 삭제 성공: 1, 실패: 0
                        Long removed = (Long) result.get(0);
                        return removed != null && removed > 0;
                    }
                    catch (Exception e) {
                        operations.discard();
                        throw e;
                    }
                }
            })));
        }
        catch (Exception e) {
            throw new BusinessException("검색어 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 전체 검색 기록 삭제
    public void deleteAllSearchHistory() {
        User loggedInUser = loginUserProvider.getLoggedInUser();
        String key = generateKey(loggedInUser.getId());

        try {
            redisTemplate.execute(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    try {
                        operations.multi();
                        operations.delete(key);
                        List<Object> result = operations.exec();

                        if(result.isEmpty())
                            throw new BusinessException("검색 기록 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

                        return result;
                    }
                    catch (Exception e) {
                        operations.discard();
                        throw e;
                    }
                }
            });
        }
        catch (Exception e) {
            throw new BusinessException("전체 검색 기록 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 유저별 redis key 생성
    private String generateKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
