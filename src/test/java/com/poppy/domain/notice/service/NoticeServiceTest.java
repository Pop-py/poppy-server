package com.poppy.domain.notice.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.notice.dto.NoticeDetailRspDto;
import com.poppy.domain.notice.dto.NoticeRspDto;
import com.poppy.domain.notice.entity.Notice;
import com.poppy.domain.notice.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {
    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Test
    void 공지사항_생성_성공() {
        // given
        String title = "[공지] 테스트 공지";
        String content = "테스트 내용";
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .build();

        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);

        // when
        NoticeRspDto result = noticeService.createNotice(title, content);

        // then
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getContent()).isEqualTo(content);
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    void 공지사항_수정_성공() {
        // given
        Long noticeId = 1L;
        String oldTitle = "[공지] 이전 제목";
        String oldContent = "이전 내용";
        String newTitle = "[공지] 새로운 제목";
        String newContent = "새로운 내용";

        Notice notice = Notice.builder()
                .id(noticeId)
                .title(oldTitle)
                .content(oldContent)
                .build();

        when(noticeRepository.findById(noticeId)).thenReturn(Optional.of(notice));

        // when
        NoticeRspDto result = noticeService.updateNotice(noticeId, newTitle, newContent);

        // then
        assertThat(result.getTitle()).isEqualTo(newTitle);
        assertThat(result.getContent()).isEqualTo(newContent);
        verify(noticeRepository).findById(noticeId);
    }

    @Test
    void 공지사항_수정시_존재하지_않는_공지사항이면_예외발생() {
        // given
        Long noticeId = 1L;
        when(noticeRepository.findById(noticeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.updateNotice(noticeId, "title", "content"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 공지를 찾을 수 없습니다");
    }

    @Test
    void 공지사항_목록_최신순_30개_조회_성공() {
        // given
        List<Notice> notices = IntStream.range(0, 3)
                .mapToObj(i -> Notice.builder()
                        .id((long) i)
                        .title("[공지] 제목" + i)
                        .content("내용" + i)
                        .build())
                .collect(Collectors.toList());

        when(noticeRepository.findTop30ByOrderByCreateTimeDesc()).thenReturn(notices);

        // when
        List<NoticeRspDto> result = noticeService.getNotices();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTitle()).isEqualTo("[공지] 제목0");
        verify(noticeRepository).findTop30ByOrderByCreateTimeDesc();
    }

    @Test
    void 공지사항_상세조회_성공() {
        // given
        Long noticeId = 1L;
        Notice notice = Notice.builder()
                .id(noticeId)
                .title("[공지] 테스트 공지")
                .content("테스트 내용")
                .build();

        when(noticeRepository.findById(noticeId)).thenReturn(Optional.of(notice));

        // when
        NoticeDetailRspDto result = noticeService.getNoticeDetail(noticeId);

        // then
        assertThat(result.getTitle()).isEqualTo(notice.getTitle());
        assertThat(result.getContent()).isEqualTo(notice.getContent());
        verify(noticeRepository).findById(noticeId);
    }

    @Test
    void 공지사항_상세조회시_존재하지_않는_공지사항이면_예외발생() {
        // given
        Long noticeId = 1L;
        when(noticeRepository.findById(noticeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNoticeDetail(noticeId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 공지를 찾을 수 없습니다");
    }
}