package com.poppy.domain.scrap.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.scrap.dto.ScrapRspDto;
import com.poppy.domain.scrap.entity.Scrap;
import com.poppy.domain.scrap.repository.ScrapRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProviderImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
            if (!isLocked) {
                throw new BusinessException(ErrorCode.SCRAP_CONFLICT);
            }

            User user = loginProvider.getLoggedInUser();
            PopupStore store = popupStoreRepository.findById(storeId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

            boolean isScraped = scrapRepository.existsByUserAndPopupStore(user, store);
            String cacheKey = SCRAP_COUNT_KEY + storeId;

            if (isScraped) {
                scrapRepository.deleteByUserAndPopupStore(user, store);
                updateScrapCount(store, cacheKey, false);
            } else {
                scrapRepository.save(Scrap.builder()
                        .user(user)
                        .popupStore(store)
                        .build());
                updateScrapCount(store, cacheKey, true);
            }

            Integer currentCount = getCurrentScrapCount(store, cacheKey);
            return ScrapRspDto.of(!isScraped, currentCount);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SCRAP_CONFLICT);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateScrapCount(PopupStore store, String cacheKey, boolean isIncrement) {
        Integer cachedCount = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount == null) {
            cachedCount = store.getScrapCount();
            redisTemplate.opsForValue().set(cacheKey, cachedCount);
        }

        if (isIncrement) {
            redisTemplate.opsForValue().increment(cacheKey);
            store.updateScrapCount(store.getScrapCount() + 1);
        } else {
            redisTemplate.opsForValue().decrement(cacheKey);
            store.updateScrapCount(store.getScrapCount() - 1);
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
}