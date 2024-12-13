package com.poppy.domain.notification.service;

import com.poppy.domain.notification.entity.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageGenerator {
    // FCM 알림의 제목 생성
    public String generateFCMTitle(NotificationType type, String storeName) {
        return switch (type) {
            case WAITING_CALL, TEAMS_AHEAD -> storeName;
            case WAITING_CANCEL -> "대기 취소 알림";
            case WAITING_TIMEOUT -> "대기 시간 초과";
            default -> throw new IllegalArgumentException("Unknown notification.html type: " + type);
        };
    }

    // FCM 알림의 내용 생성
    public String generateFCMBody(NotificationType type, Integer waitingNumber, Integer peopleAhead) {
        return switch (type) {
            case WAITING_CALL -> "지금 바로 입장해 주세요";
            case WAITING_CANCEL -> String.format("%d번 대기가 취소되었습니다", waitingNumber);
            case TEAMS_AHEAD -> String.format("현재 %d번째 순서\n대기번호 %d번", peopleAhead, waitingNumber);
            case WAITING_TIMEOUT -> String.format("%d번 대기가 시간 초과로 취소되었습니다", waitingNumber);
        };
    }

    // WebSocket 실시간 알림 메시지 생성
    public String generateWebSocketMessage(NotificationType type, String storeName, Integer waitingNumber, Integer peopleAhead) {
        return switch (type) {
            case WAITING_CALL -> String.format("%s\n고객님의 입장 순서입니다.\n%d번 고객님은 카운터로 와주세요.", storeName, waitingNumber);
            case WAITING_CANCEL -> String.format("%s\n%d번 대기가 취소되었습니다.", storeName, waitingNumber);
            case TEAMS_AHEAD -> {
                if (peopleAhead <= 3)
                    yield String.format("%s\n앞으로 %d팀 남았습니다.\n잠시 후 입장 예정이니 매장 앞에서 대기해 주세요.", storeName, peopleAhead);
                yield String.format("%s\n현재 %d번째 순서입니다.", storeName, peopleAhead);
            }
            case WAITING_TIMEOUT -> String.format("%s\n%d번 대기\n호출 시간 초과로 자동 취소되었습니다.", storeName, waitingNumber);
        };
    }
}
