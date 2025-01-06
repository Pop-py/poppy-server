package com.poppy.domain.search.service;

import com.poppy.common.config.redis.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HourlyKeywordScheduler {
    private static final String HOURLY_KEYWORD_LOCK = "hourly-keyword-lock";

    private final PopularKeywordService popularKeywordService;
    private final DistributedLockService lockService;

    @Scheduled(cron = "0 0 * * * *")  // 매 시 정각에 실행
    public void updateHourlyKeywords() {
        try {
            // 락 획득 시도
            if (lockService.tryLock(HOURLY_KEYWORD_LOCK)) {
                try {
                    popularKeywordService.switchHourlyKeywords();
                    log.info("시간별 인기 검색어가 업데이트 완료.");
                } catch (Exception e) {
                    log.error("시간별 인기 검색어 업데이트에 실패.");
                } finally {
                    // 락 해제
                    lockService.unlock(HOURLY_KEYWORD_LOCK);
                }
            } else {
                log.info("다른 인스턴스에서 인기 검색어 업데이트를 처리 중");
            }
        } catch (Exception e) {
            log.error("인기 검색어 업데이트 중 오류 발생: ", e);
        }
    }
}
