package com.poppy.domain.waiting.service;

import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WaitingScheduler {
    private final WaitingRepository waitingRepository;
    private final MasterWaitingService masterWaitingService;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void checkWaitingTimeout() {
        List<Waiting> waitingList = waitingRepository.findByStatus(WaitingStatus.CALLED);
        LocalDateTime now = LocalDateTime.now();

        for (Waiting waiting : waitingList) {
            LocalDateTime calledTime = waiting.getUpdateTime(); // 호출된 시간
            if (calledTime.plusMinutes(MasterWaitingService.WAITING_TIMEOUT_MINUTES).isBefore(now)) {
                masterWaitingService.handleWaitingTimeout(waiting.getId());
            }
        }
    }
}