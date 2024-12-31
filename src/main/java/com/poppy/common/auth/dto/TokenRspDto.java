package com.poppy.common.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;

@Getter
@Builder
public class TokenRspDto {
    private String accessToken;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone="Asia/Seoul")
    private Date accessTokenExp;

    private String refreshToken;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss", timezone="Asia/Seoul")
    private Date refreshTokenExp;

    private String userEmail;

    private String nickname;
}
