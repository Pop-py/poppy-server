package com.poppy.domain.waiting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
    private final RedissonClient redissonClient;
    public static final String SCHEDULER_LOCK_KEY = "waiting-scheduler-lock";
    private static final long LOCK_WAIT_TIME = 5L;
    private static final long LOCK_LEASE_TIME = 60L;

    public boolean tryLock(String lockName) {
        RLock lock = redissonClient.getLock(lockName);
        try {
            // 5초 동안 락 획득 시도, 획득하면 60초 동안 락 유지
            return lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to acquire lock: {}", e.getMessage());
            return false;
        }
    }

    public void unlock(String lockName) {
        RLock lock = redissonClient.getLock(lockName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}