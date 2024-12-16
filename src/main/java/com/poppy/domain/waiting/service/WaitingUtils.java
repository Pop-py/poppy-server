package com.poppy.domain.waiting.service;

import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WaitingUtils {
    private final WaitingRepository waitingRepository;
    private final NotificationService notificationService;

    // 대기 순서가 변경된 대기자들에게 대기 순서 알림을 발송
    public void updateWaitingQueue(Long storeId, Long waitingId) {
        List<Waiting> waitingList = waitingRepository
                .findByPopupStoreIdAndStatusAndWaitingNumberGreaterThanOrderByWaitingNumberAsc(
                        storeId,
                        WaitingStatus.WAITING,
                        waitingRepository.findById(waitingId)
                                .map(Waiting::getWaitingNumber)
                                .orElse(0)
                );

        for (Waiting waiting : waitingList) {
            int peopleAhead = waitingRepository.countPeopleAhead(
                    storeId,
                    waiting.getWaitingNumber(),
                    Set.of(WaitingStatus.WAITING, WaitingStatus.CALLED) // 'WAITING' 또는 'CALLED' 상태인 대기자들만 포함
            );
            notificationService.sendNotification(waiting, NotificationType.TEAMS_AHEAD, peopleAhead);
        }
    }
}