package com.poppy.domain.user.repository;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUserProviderImpl implements LoginUserProvider {
    private final UserRepository userRepository;

    @Override
    public User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String userIdStr = authentication.getName(); // Authentication의 Principal에서 사용자 ID 가져오기

        try {
            return userRepository.findById(Long.parseLong(userIdStr))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }
        catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public User getLoggedInUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser"))
            return null;

        String userIdStr = authentication.getName();

        try {
            return userRepository.findById(Long.parseLong(userIdStr)).orElse(null);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
