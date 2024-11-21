package com.poppy.domain.user.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import com.poppy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 로그인/회원가입
    @Transactional
    public User login(String email, String nickname) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        // 가입이 안 되어 있을 경우 회원가입
        if(optionalUser.isEmpty()) {
            // 이메일 중복 확인
            if (userRepository.findByEmail(email).isPresent()) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }

            user = User.builder()
                    .email(email)
                    .phoneNumber("")
                    .nickname(nickname)
                    .oauthProvider("naver")
                    .role(Role.ROLE_USER)
                    .build();

            userRepository.save(user);
        }
        // 가입이 되어 있으면 로그인
        else {
            user = optionalUser.get();
            user.updateLoginInfo(nickname, "naver", Role.ROLE_USER);
        }

        return user;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 로그인한 유저 확인
    public User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String userIdStr = authentication.getName(); // Authentication의 Principal에서 사용자 ID 가져오기
        System.out.println("로그인 유저 확인용" + userIdStr);

        return userRepository.findById(Long.parseLong(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
