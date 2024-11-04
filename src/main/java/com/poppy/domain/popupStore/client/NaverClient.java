package com.poppy.domain.popupStore.client;


import com.poppy.common.config.external.NaverConfig;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.response.NaverApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class NaverClient {
    private final NaverConfig naverConfig;
    private final RestTemplate restTemplate;

    public NaverApiResponseDto searchLocal(String query) {
        // 요청 URI 생성
        var uri = UriComponentsBuilder.fromUriString(naverConfig.getUrl().getSearch().getLocal())
                .queryParam("query", query)
                .queryParam("display", 5) // 검색 결과 개수 설정
                .build()
                .encode()
                .toUri();

        // 요청 헤더 설정
        var headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", naverConfig.getClient().getId());
        headers.set("X-Naver-Client-Secret", naverConfig.getClient().getSecret());

        var httpEntity = new HttpEntity<>(headers);
        // RestTemplate으로 네이버 API 호출
        var responseEntity = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                NaverApiResponseDto.class
        );

        if (responseEntity.getBody() == null) {
            throw new BusinessException(ErrorCode.NAVER_API_ERROR);
        }

        return responseEntity.getBody();
    }
}