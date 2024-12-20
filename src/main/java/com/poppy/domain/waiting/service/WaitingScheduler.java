package com.poppy.domain.waiting.service;

import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaitingScheduler {
    private final WaitingRepository waitingRepository;
    private final MasterWaitingService masterWaitingService;
    private final DistributedLockService lockService;
    private static final String SCHEDULER_LOCK_KEY = "waiting-scheduler-lock";

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void checkWaitingTimeout() {
        // 분산 락 획득 시도
        if (!lockService.tryLock(SCHEDULER_LOCK_KEY)) {
            log.debug("Failed to acquire scheduler lock. Skipping this execution.");
            return;
        }

        try {
            List<Waiting> waitingList = waitingRepository.findByStatus(WaitingStatus.CALLED);
            LocalDateTime now = LocalDateTime.now();

            for (Waiting waiting : waitingList) {
                LocalDateTime calledTime = waiting.getUpdateTime();
                if (calledTime.plusMinutes(MasterWaitingService.WAITING_TIMEOUT_MINUTES).isBefore(now)) {
                    masterWaitingService.handleWaitingTimeout(waiting.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduler execution: {}", e.getMessage(), e);
        } finally {
            lockService.unlock(SCHEDULER_LOCK_KEY);
        }
    }
}