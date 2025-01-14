package com.poppy.domain.scrap.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.scrap.dto.ScrapRspDto;
import com.poppy.domain.scrap.dto.UserScrapRspDto;
import com.poppy.domain.scrap.entity.ScrapSortType;
import com.poppy.domain.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scraps")
@RequiredArgsConstructor
public class ScrapController {
    private final ScrapService scrapService;

    @PostMapping("/{storeId}")
    public RspTemplate<ScrapRspDto> toggleScrap(@PathVariable Long storeId) {
        return new RspTemplate<>(HttpStatus.OK, "팝업스토어 저장 여부 확인", scrapService.toggleScrap(storeId));
    }

    // 유저의 팝업 스토어 스크랩 상태 확인
    @GetMapping("/{storeId}")
    public RspTemplate<ScrapRspDto> getScrapStatus(@PathVariable Long storeId) {
        return new RspTemplate<>(HttpStatus.OK, "스크랩 상태 조회 완료", scrapService.getScrapStatus(storeId));
    }

    // 유저의 팝업 스토어 스크랩 목록
    @GetMapping
    public RspTemplate<List<UserScrapRspDto>> getUserScraps(@RequestParam(defaultValue = "RECENT_SAVED") ScrapSortType sortType) {
        return new RspTemplate<>(HttpStatus.OK, "스크랩 목록 조회", scrapService.getUserScraps(sortType));
    }
}