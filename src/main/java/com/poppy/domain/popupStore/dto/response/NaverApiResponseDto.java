package com.poppy.domain.popupStore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NaverApiResponseDto {
    private List<Item> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String title;
        private String address;
        @JsonProperty("roadAddress")
        private String roadAddress;
        private String mapx; // 경도
        private String mapy; // 위도
    }
}
