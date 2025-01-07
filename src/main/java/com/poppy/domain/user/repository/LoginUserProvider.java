package com.poppy.domain.user.repository;

import com.poppy.domain.user.entity.User;

public interface LoginUserProvider {    // 로그인 유저 판단하기 위한 인터페이스
    User getLoggedInUser();     // 로그인한 유저를 찾지 못하면 에러
    User getLoggedInUserOrNull();   // 로그인한 유저를 찾지 못하면 null
}
