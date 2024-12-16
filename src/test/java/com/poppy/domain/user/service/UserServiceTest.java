package com.poppy.domain.user.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("테스터")
                .phoneNumber("010-1234-5678")
                .role(Role.ROLE_USER)
                .build();
    }

    @Nested
    class FCM토큰_업데이트_테스트 {
        @Test
        void FCM토큰_업데이트_성공() {
            // given
            String newToken = "new-fcm-token";
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // when
            userService.updateFcmToken(1L, newToken, 1L);

            // then
            assertEquals(newToken, user.getFcmToken());
            verify(userRepository).save(user);
        }

        @Test
        void FCM토큰_업데이트_실패_사용자없음() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThrows(BusinessException.class, () ->
                    userService.updateFcmToken(1L, "new-token", 1L));
        }

        @Test
        void FCM토큰_업데이트_실패_권한없음() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when & then
            assertThrows(BusinessException.class, () ->
                    userService.updateFcmToken(1L, "new-token", 2L));
        }
    }
}