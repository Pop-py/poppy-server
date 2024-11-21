package com.poppy.domain.reservation.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.popupStore.service.PopupStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class TestController {
    private final PopupStoreService popupStoreService;

    // 나중에 관리자 컨트롤러에 넣어야 함
    // 슬롯 초기화 (관리자가 팝업 스토어 등록 시 실행)
    @PostMapping("/{storeId}/initialize")
    public RspTemplate<String> initializeSlots(@PathVariable Long storeId) {
        popupStoreService.initializeSlots(storeId);
        return new RspTemplate<>(HttpStatus.OK, "팝업 스토어 슬롯 초기화 완료");
    }
}
