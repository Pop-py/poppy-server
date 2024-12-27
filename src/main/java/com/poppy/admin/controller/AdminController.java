package com.poppy.admin.controller;

import com.poppy.admin.service.AdminService;
import com.poppy.common.api.RspTemplate;
import com.poppy.domain.popupStore.dto.request.PopupStoreReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/popup-stores")
    public RspTemplate<PopupStoreRspDto> registerPopUpStore(@Valid @RequestBody @ModelAttribute PopupStoreReqDto reqDto){
        PopupStoreRspDto popupStoreRspDto = adminService.savePopupStore(reqDto);
        return new RspTemplate<>(HttpStatus.OK, "팝업스토어 등록 완료", popupStoreRspDto);
    }

    @DeleteMapping("/popup-stores/{id}")
    public RspTemplate<Void> deletePopUpStore(@PathVariable Long id) {
        adminService.deletePopupStore(id);
        return new RspTemplate<>(HttpStatus.OK, "팝업스토어 삭제 완료");
    }
}
