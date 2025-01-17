package com.poppy.domain.popupStore.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.popupStore.dto.request.PopupStoreSearchReqDto;
import com.poppy.domain.popupStore.dto.request.PopupStoreUpdateReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreCalenderRspDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.dto.response.ReservationAvailableSlotRspDto;
import com.poppy.domain.popupStore.service.PopupStoreService;
import com.poppy.domain.search.service.StoreSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/popup-stores")
@RequiredArgsConstructor
public class PopupStoreController {
    private final PopupStoreService popupStoreService;
    private final StoreSearchService storeSearchService;

    // 전체 목록 조회
    @GetMapping
    public RspTemplate<List<PopupStoreRspDto>> getAllStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 목록 조회 성공",
                popupStoreService.getAllActiveStores()
        );
    }

    // 팝업스토어 상세 조회
    @GetMapping("/detail/{id}")
    public RspTemplate<PopupStoreRspDto> getStoreDetail(@PathVariable Long id) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 상세 조회 성공",
                popupStoreService.getPopupStore(id)
        );
    }

    // 마감 임박 팝업스토어 조회
    @GetMapping("/deadline")
    public RspTemplate<List<PopupStoreRspDto>> getDeadLineStores(){
        return new RspTemplate<>(HttpStatus.OK,
                "마감이 임박한 팝업스토어 조회 성공",
                popupStoreService.getDeadlinePopupStores());
    }

    // 3시간 내 인기 팝업 스토어 조회
    @GetMapping("/popular")
    public RspTemplate<List<PopupStoreRspDto>> getPopularStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "3시간 내 인기 팝업스토어 조회 성공",
                popupStoreService.getPopularPopupStores()
        );
    }

    // 현재 많이 찾는 (카테고리) 팝업스토어 조회
    @GetMapping("/popular/category/{categoryId}")
    public RspTemplate<List<PopupStoreRspDto>> getPopularStoresByCategory(@PathVariable Long categoryId) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "3시간 내 카테고리별 인기 팝업스토어 조회 성공",
                popupStoreService.getPopularPopupStoresByCategory(categoryId)
        );
    }

    // 오픈 예정 팝업스토어
    @GetMapping("/future")
    public RspTemplate<List<PopupStoreRspDto>> getFutureStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "오픈 예정 팝업스토어 조회 성공",
                popupStoreService.getAllFuturePopupStores()
        );
    }

    // 팝업 스토어 검색 필터링
    @GetMapping("/search")
    public RspTemplate<List<PopupStoreRspDto>> searchStores(PopupStoreSearchReqDto reqDto) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 검색 성공",
                popupStoreService.searchFiltering(reqDto)
        );
    }

    // 이름으로 검색
    @GetMapping("/{name}")
    public RspTemplate<List<PopupStoreRspDto>> searchStores(@PathVariable String name) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "키워드 '" + name + "' 검색 성공",
                storeSearchService.searchStoresAndSaveHistory(name)
        );
    }

    // 신규 스토어 조회
    @GetMapping("/new")
    public RspTemplate<List<PopupStoreRspDto>> getNewStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "신규 팝업스토어 조회 성공",
                popupStoreService.getNewStores()
        );
    }

    // 비슷한 팝업 스토어 조회
    @GetMapping("/detail/{id}/similar")
    public RspTemplate<List<PopupStoreRspDto>> getSimilarStores(@PathVariable Long id) {
        return new RspTemplate<>(HttpStatus.OK, "비슷한 팝업 스토어 조회", popupStoreService.getSimilarStore(id));
    }

    // 팝업스토어 예약 가능 날짜 조회
    @GetMapping("/{id}/calendar")
    public RspTemplate<PopupStoreCalenderRspDto> getCalender(@PathVariable Long id) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "캘린더 조회 성공",
                popupStoreService.getCalender(id)
        );
    }

    // 특정 날짜의 예약 가능 시간 조회
    @GetMapping("/{storeId}/{date}")
    public RspTemplate<List<ReservationAvailableSlotRspDto>> getAvailable(
            @PathVariable Long storeId, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        PopupStoreRspDto popupStore = popupStoreService.getPopupStore(storeId);
        List<ReservationAvailableSlotRspDto> available = popupStoreService.getAvailableSlots(storeId, date);
        return new RspTemplate<>(HttpStatus.OK, popupStore.getName() + "의 예약 가능 시간 조회", available);
    }

    // 특정 구역으로 검색
    @GetMapping("/address/{address}")
    public RspTemplate<List<PopupStoreRspDto>> getStoresByDistrict(@PathVariable String address) {
        return new RspTemplate<>(
                HttpStatus.OK,
                address + " 지역 팝업스토어 조회 성공",
                popupStoreService.getStoresByAddress(address)
        );
    }

    // 팝업스토어 수정
    @PatchMapping("/{id}")
    public RspTemplate<PopupStoreRspDto> updatePopupStore(
            @PathVariable Long id,
            @Valid @ModelAttribute PopupStoreUpdateReqDto reqDto) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 수정 성공",
                popupStoreService.updatePopupStore(id, reqDto)
        );
    }
}