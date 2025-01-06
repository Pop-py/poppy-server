package com.poppy.domain.search.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.search.service.PopularKeywordService;
import com.poppy.domain.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search-history")
public class SearchHistoryController {
    private final SearchHistoryService searchHistoryService;
    private final PopularKeywordService popularKeywordService;

    @GetMapping
    public RspTemplate<List<String>> getSearchHistory() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "검색 기록 조회 성공",
                searchHistoryService.getSearchHistory()
        );
    }

    // 인기 검색어 조회
    @GetMapping("/popular")
    public RspTemplate<List<String>> getPopularSearchHistory() {
        LocalDateTime now = LocalDateTime.now();
        String message = String.format(
                "%d월 %d일 %d시 인기 검색어 조회",
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour()
        );

        return new RspTemplate<>(
                HttpStatus.OK,
                message,
                popularKeywordService.getTopKeywords()
        );
    }

    @DeleteMapping("/{keyword}")
    public RspTemplate<Void> deleteSearchKeyword(@PathVariable String keyword) {
        boolean isDeleted = searchHistoryService.deleteSearchKeyword(keyword);

        if(isDeleted) return new RspTemplate<>(HttpStatus.OK, "검색 기록 삭제 성공");
        else return new RspTemplate<>(HttpStatus.NO_CONTENT, "삭제할 키워드가 존재하지 않습니다.");
    }

    @DeleteMapping
    public RspTemplate<Void> deleteAllSearchHistory() {
        searchHistoryService.deleteAllSearchHistory();
        return new RspTemplate<>(HttpStatus.OK, "전체 검색 기록 삭제 성공");
    }
}
