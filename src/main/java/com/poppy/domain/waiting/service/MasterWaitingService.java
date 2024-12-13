package com.poppy.domain.waiting.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notification.entity.NotificationType;
import com.poppy.domain.notification.service.NotificationService;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import com.poppy.domain.waiting.dto.response.DailyWaitingRspDto;
import com.poppy.domain.waiting.dto.response.WaitingRspDto;
import com.poppy.domain.waiting.entity.Waiting;
import com.poppy.domain.waiting.entity.WaitingStatus;
import com.poppy.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterWaitingService {
    public static final int WAITING_TIMEOUT_MINUTES = 5;

    private final WaitingRepository waitingRepository;
    private final NotificationService notificationService;
    private final WaitingUtils waitingUtils;
    private final PopupStoreRepository popupStoreRepository;
    private final LoginUserProvider loginUserProvider;

    // 날짜별 대기 목록 조회
    @Transactional(readOnly = true)
    public List<DailyWaitingRspDto> getDailyWaitings(Long storeId, LocalDate date) {
        validateMasterAuthority(storeId);
        return waitingRepository.findWaitingsByStoreIdAndDate(storeId, date.atStartOfDay())
                .stream()
                .map(DailyWaitingRspDto::from)
                .collect(Collectors.toList());
    }

    // 대기 상태 업데이트 후 활성 목록 반환
    @Transactional
    public List<WaitingRspDto> updateWaitingStatus(Long storeId, Long waitingId, WaitingStatus newStatus) {
        validateMasterAuthority(storeId);

        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAITING_NOT_FOUND));

        if (!waiting.getPopupStore().getId().equals(storeId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_WAITING_ACCESS);
        }

        waiting.updateStatus(newStatus);

        switch (newStatus) {
            case CALLED:
                notificationService.sendNotification(waiting, NotificationType.WAITING_CALL, null);
                waitingUtils.updateWaitingQueue(waiting.getPopupStore().getId(), waitingId);
                break;
            case COMPLETED:
                waitingUtils.updateWaitingQueue(waiting.getPopupStore().getId(), waitingId);
                break;
        }

        return getActiveWaitings(storeId);
    }

    // 활성화된 대기 목록 조회
    @Transactional(readOnly = true)
    public List<WaitingRspDto> getActiveWaitings(Long storeId) {
        validateMasterAuthority(storeId);
        return waitingRepository.findActiveWaitings(
                        storeId,
                        Set.of(WaitingStatus.WAITING, WaitingStatus.CALLED)
                ).stream()
                .map(WaitingRspDto::from)
                .collect(Collectors.toList());
    }

    // 타임아웃으로 인한 대기 취소 처리
    @Transactional
    public void handleWaitingTimeout(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAITING_NOT_FOUND));

        validateMasterAuthority(waiting.getPopupStore().getId());

        if (waiting.getStatus() != WaitingStatus.CALLED) {
            return;
        }

        waiting.updateStatus(WaitingStatus.CANCELED);
        notificationService.sendNotification(waiting, NotificationType.WAITING_TIMEOUT, null);
        waitingUtils.updateWaitingQueue(waiting.getPopupStore().getId(), waitingId);

        log.info("Waiting timeout canceled - waitingId: {}, waitingNumber: {}",
                waiting.getId(), waiting.getWaitingNumber());
    }

    private void validateMasterAuthority(Long storeId) {
        User master = loginUserProvider.getLoggedInUser();
        PopupStore store = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (!store.getMasterUser().getId().equals(master.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_STORE_ACCESS);
        }
    }
}