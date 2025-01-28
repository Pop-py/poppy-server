package com.poppy.domain.scrap.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.scrap.dto.response.ScrapRspDto;
import com.poppy.domain.scrap.dto.response.UserScrapRspDto;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.scrap.entity.ScrapSortType;
import com.poppy.domain.scrap.repository.ScrapRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProviderImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapService {
    private static final String SCRAP_COUNT_KEY = "store:scrap:count:";
    private static final String LOCK_KEY = "store:scrap:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 3L;

    private final ScrapRepository scrapRepository;
    private final PopupStoreRepository popupStoreRepository;
    private final LoginUserProviderImpl loginProvider;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Integer> redisTemplate;

    @Transactional
    public ScrapRspDto toggleScrap(Long storeId) {
        RLock lock = redissonClient.getLock(LOCK_KEY + storeId);

        try {
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) throw new BusinessException(ErrorCode.SCRAP_CONFLICT);

            User user = loginProvider.getLoggedInUser();
            PopupStore store = popupStoreRepository.findById(storeId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

            boolean isScraped = scrapRepository.existsByUserAndPopupStore(user, store);
            String cacheKey = SCRAP_COUNT_KEY + storeId;

            int currentCount = store.getScrapCount();   // 현재 스크랩 수 조회

            if (isScraped) {
                store.updateScrapCount(currentCount - 1);
                scrapRepository.deleteByUserAndPopupStore(user, store);
                redisTemplate.opsForValue().decrement(cacheKey);
            }
            else {
                store.updateScrapCount(currentCount + 1);
                scrapRepository.save(Scrap.builder()
                        .user(user)
                        .popupStore(store)
                        .build());
                redisTemplate.opsForValue().increment(cacheKey);
            }

            // 명시적 저장 추가
            popupStoreRepository.saveAndFlush(store);

            return ScrapRspDto.of(!isScraped, store.getScrapCount());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SCRAP_CONFLICT);
        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Integer getCurrentScrapCount(PopupStore store, String cacheKey) {
        Integer cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount == null) {
            cachedCount = store.getScrapCount();
            redisTemplate.opsForValue().set(cacheKey, cachedCount);
        }
        return cachedCount;
    }

    @Transactional(readOnly = true)
    public ScrapRspDto getScrapStatus(Long storeId) {
        User user = loginProvider.getLoggedInUser();
        PopupStore store = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        String cacheKey = SCRAP_COUNT_KEY + storeId;
        Integer scrapCount = getCurrentScrapCount(store, cacheKey);

        return ScrapRspDto.of(
                scrapRepository.existsByUserAndPopupStore(user, store),
                scrapCount
        );
    }

    @Transactional(readOnly = true)
    public List<UserScrapRspDto> getUserScraps(ScrapSortType scrapSortType) {
        User user = loginProvider.getLoggedInUser();
        return scrapRepository.findScrapsByUserAndSortType(user, scrapSortType.name())
                .stream()
                .map(UserScrapRspDto::from)
                .toList();
    }

    // 스크랩 삭제
    @Transactional
    public void deleteScrap(List<Long> scrapIds) {
        User user = loginProvider.getLoggedInUser();

        // 중복 제거
        List<Long> uniqueScrapIds = scrapIds.stream()
                .distinct()
                .toList();

        // 해당 유저의 스크랩 목록만 조회
        List<Scrap> scraps = scrapRepository.findByUserIdAndIdIn(user.getId(), uniqueScrapIds);

        // 요청된 모든 스크랩이 존재하는지 확인
        if(scraps.size() != uniqueScrapIds.size())
            throw new BusinessException(ErrorCode.SCRAP_NOT_FOUND);

        // PopupStore별로 스크랩 그룹화
        Map<PopupStore, Long> storeScrapCounts = scraps.stream()
                .collect(Collectors.groupingBy(
                        Scrap::getPopupStore,
                        Collectors.counting()
                ));

        // 각 PopupStore의 스크랩 카운트 업데이트
        storeScrapCounts.forEach((store, count) -> {
            String cacheKey = SCRAP_COUNT_KEY + store.getId();
            int currentCount = store.getScrapCount();

            store.updateScrapCount(currentCount - count.intValue());
            redisTemplate.opsForValue().decrement(cacheKey, count);
            popupStoreRepository.saveAndFlush(store);
        });

        // 스크랩 일괄 삭제
        scrapRepository.deleteAllById(scrapIds);
    }
}