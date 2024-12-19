package com.poppy.domain.scrap.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.scrap.dto.ScrapRspDto;
import com.poppy.domain.scrap.service.ScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/{storeId}")
    public RspTemplate<ScrapRspDto> toggleScrap(
            @PathVariable Long storeId
    ) {
        return new RspTemplate<>(HttpStatus.OK,"팝업스토어 저장 완료",scrapService.toggleScrap(storeId));
    }

    @GetMapping("/{storeId}")
    public RspTemplate<ScrapRspDto> getScrapStatus(
            @PathVariable Long storeId
    ) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "스크랩 상태 조회 완료",
                scrapService.getScrapStatus(storeId)
        );
    }
}