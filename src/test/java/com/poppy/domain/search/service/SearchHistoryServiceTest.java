package com.poppy.domain.search.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.LoginUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {
    @Mock
    private LoginUserProvider loginUserProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    private User mockUser;
    private static final String TEST_KEYWORD = "테스트";
    private static final String KEY_PREFIX = "search:history:";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .build();
    }

    @Test
    void 검색어_저장_성공() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenReturn(List.of(true));

        // when & then
        assertDoesNotThrow(() -> searchHistoryService.saveSearchHistory(TEST_KEYWORD));
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void 검색어_저장_실패_시_Redis_확인() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenThrow(new BusinessException("검색어 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> searchHistoryService.saveSearchHistory(TEST_KEYWORD));

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getCode());
        assertEquals("검색어 저장에 실패했습니다.", exception.getMessage());
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void 검색어_조회_성공() {
        // given
        Set<String> mockHistory = new HashSet<>(Arrays.asList("검색어1", "검색어2"));
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRange(KEY_PREFIX + mockUser.getId(), 0, -1))
                .thenReturn(mockHistory);

        // when
        List<String> result = searchHistoryService.getSearchHistory();

        // then
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertTrue(result.containsAll(mockHistory))
        );
        verify(zSetOperations).reverseRange(anyString(), anyLong(), anyLong());
    }

    @Test
    void 검색어_부분_삭제() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            RedisOperations<String, String> operations = mock(RedisOperations.class);
            ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);

            when(operations.opsForZSet()).thenReturn(zSetOps);
            when(operations.exec()).thenReturn(List.of(1L));

            return callback.execute(operations);
        });

        // when
        boolean result = searchHistoryService.deleteSearchKeyword(TEST_KEYWORD);

        // then
        assertTrue(result);
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void 검색어_부분_삭제_실패_시_Redis_확인() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenReturn(Collections.emptyList());

        // when
        boolean result = searchHistoryService.deleteSearchKeyword(TEST_KEYWORD);

        // then
        assertFalse(result);
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void 검색어_전체_삭제() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenReturn(List.of(true));

        // when & then
        assertDoesNotThrow(() -> searchHistoryService.deleteAllSearchHistory());
        verify(redisTemplate).execute(any(SessionCallback.class));
    }

    @Test
    void 검색어_전체_삭제_실패_시_Redis_확인() {
        // given
        when(loginUserProvider.getLoggedInUser()).thenReturn(mockUser);
        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            RedisOperations<String, String> operations = mock(RedisOperations.class);

            // multi()와 exec() 메소드 모킹
            doReturn(operations).when(operations).multi();
            when(operations.exec()).thenReturn(Collections.emptyList());

            return callback.execute(operations);
        });

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> searchHistoryService.deleteAllSearchHistory());

        // 예외 메시지와 상태 코드 검증
        assertEquals("전체 검색 기록 삭제에 실패했습니다.", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getCode());
        verify(redisTemplate).execute(any(SessionCallback.class));
    }
}