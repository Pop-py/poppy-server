package com.poppy.domain.notice.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.notice.dto.NoticeDetailRspDto;
import com.poppy.domain.notice.dto.NoticeReqDto;
import com.poppy.domain.notice.dto.NoticeRspDto;
import com.poppy.domain.notice.service.NoticeService;
import com.poppy.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;
    private final NotificationService notificationService;

    @PostMapping("/admin/notices")
    public RspTemplate<NoticeRspDto> registerNotice(@RequestBody @Valid NoticeReqDto noticeReqDto) {
        NoticeRspDto noticeDto = noticeService.createNotice(noticeReqDto.getTitle(), noticeReqDto.getContent());
        notificationService.sendNotice(noticeDto);
        return new RspTemplate<>(
                HttpStatus.CREATED,
                "공지사항 작성 및 알림 전송 성공",
                noticeDto
        );
    }

    @PatchMapping("/notices/{noticeId}")
    public RspTemplate<NoticeRspDto> updateNotice(@PathVariable Long noticeId, @RequestBody NoticeReqDto noticeReqDto) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "공지사항 수정 성공",
                noticeService.updateNotice(
                        noticeId,
                        noticeReqDto.getTitle(),
                        noticeReqDto.getContent()
                )
        );
    }

    @GetMapping("/notices")
    public RspTemplate<List<NoticeRspDto>> getNotices() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "공지사항 목록 조회 성공",
                noticeService.getNotices()
        );
    }

    @GetMapping("/notices/{noticeId}")
    public RspTemplate<NoticeDetailRspDto> getNoticeDetail(@PathVariable Long noticeId) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "공지사항 상세 조회 성공",
                noticeService.getNoticeDetail(noticeId)
        );
    }
}
