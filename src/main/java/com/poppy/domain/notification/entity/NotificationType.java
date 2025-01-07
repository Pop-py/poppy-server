package com.poppy.domain.notification.entity;

public enum NotificationType {
    WAITING_CALL("대기 호출"),             // 대기자 호출 알림
    WAITING_CANCEL("대기 취소"),           // 대기 취소 알림
    TEAMS_AHEAD("대기 상태"),              // 앞 3팀 이하 알림
    WAITING_TIMEOUT("호출 시간 초과"),      // 5분 초과 자동 취소 알림
    RESERVATION_CHECK("예약 완료 알림"),
    RESERVATION_CANCEL("예약 취소 알림"),
    NOTICE("공지사항"),
    REMIND_24H("예약 24시간 전 알림"), // 예약 24시간 전 알림
    SCRAPED_STORE_OPENING("스크랩한 스토어 오픈");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }
}