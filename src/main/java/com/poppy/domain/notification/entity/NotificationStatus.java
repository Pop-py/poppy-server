package com.poppy.domain.notification.entity;

public enum NotificationStatus {
    RESERVATION_OPEN,      // 팝업 스토어 오픈 알림
    WAITING_NUMBER,        // 대기 번호 알림
    LAST_TEAM_ALERT,       // 마지막 팀 남음 알림
    RESERVATION_REMINDER,  // 예약 24시간 전 알림
    TIME_OUT               // 예약 시간 초과 알림
}