package com.poppy.domain.notice.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.notice.dto.NoticeDetailRspDto;
import com.poppy.domain.notice.dto.NoticeRspDto;
import com.poppy.domain.notice.entity.Notice;
import com.poppy.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;

    // 공지사항 생성
    @Transactional
    public NoticeRspDto createNotice(String title, String content) {
        Notice notice = noticeRepository.save(
                Notice.builder()
                        .title(title)
                        .content(content)
                        .build()
        );
        return NoticeRspDto.from(notice);
    }

    // 공지사항 수정
    @Transactional
    public NoticeRspDto updateNotice(Long noticeId, String title, String content) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        notice.update(title, content);
        return NoticeRspDto.from(notice);
    }

    // 공지 목록 조회 (최신순 30개)
    @Transactional(readOnly = true)
    public List<NoticeRspDto> getNotices() {
        return noticeRepository.findTop30ByOrderByCreateTimeDesc().stream()
                .map(NoticeRspDto::from)
                .collect(Collectors.toList());
    }

    // 공지 상세 조회
    @Transactional(readOnly = true)
    public NoticeDetailRspDto getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        return NoticeDetailRspDto.builder()
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdDate(notice.getCreateTime().toLocalDate())
                .build();
    }
}
