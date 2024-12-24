package com.poppy.domain.waiting.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.waiting.dto.request.UpdateWaitingStatusReqDto;
import com.poppy.domain.waiting.dto.request.WaitingSettingsReqDto;
import com.poppy.domain.waiting.dto.response.DailyWaitingRspDto;
import com.poppy.domain.waiting.dto.response.WaitingRspDto;
import com.poppy.domain.waiting.dto.response.WaitingSettingsRspDto;
import com.poppy.domain.waiting.service.MasterWaitingService;
import com.poppy.domain.waiting.service.WaitingSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/master/waiting/popup-stores/{storeId}")
@RequiredArgsConstructor
public class MasterWaitingController {
    private final MasterWaitingService masterWaitingService;
    private final WaitingSettingsService waitingSettingsService;

    @GetMapping("/settings")
    public RspTemplate<WaitingSettingsRspDto> getSettings(@PathVariable Long storeId) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "대기 설정 조회 성공",
                waitingSettingsService.getSettings(storeId)
        );
    }

    @PatchMapping("/settings")
    public RspTemplate<WaitingSettingsRspDto> updateSettings(
            @PathVariable Long storeId,
            @Valid @RequestBody WaitingSettingsReqDto waitingSettingsReqDto) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "대기 설정이 수정되었습니다",
                waitingSettingsService.updateSettings(storeId, waitingSettingsReqDto)
        );
    }

    @PatchMapping("/waitings/{waitingId}/status")
    public RspTemplate<List<WaitingRspDto>> updateWaitingStatus(
            @PathVariable Long storeId,
            @PathVariable Long waitingId,
            @Valid @RequestBody UpdateWaitingStatusReqDto updateWaitingStatusReqDto) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "대기 상태가 변경되었습니다.",
                masterWaitingService.updateWaitingStatus(storeId, waitingId, updateWaitingStatusReqDto.getStatus())
        );
    }

    @GetMapping("/waitings")
    public RspTemplate<List<DailyWaitingRspDto>> getWaitingHistory(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "일일 대기 현황 조회 성공",
                masterWaitingService.getDailyWaitings(storeId, date)
        );
    }

    @GetMapping("/waitings/hourly")
    public RspTemplate<List<DailyWaitingRspDto>> getHourlyWaitingHistory(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @RequestParam int hour) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "시간대별 대기 현황 조회 성공",
                masterWaitingService.getHourlyWaitings(storeId, date, hour)
        );
    }
}
