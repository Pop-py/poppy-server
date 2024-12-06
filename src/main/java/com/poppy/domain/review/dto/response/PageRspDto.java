package com.poppy.domain.review.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageRspDto<T> {
    private List<T> content;
    private PageMetadata metadata;

    public PageRspDto(Page<T> page) {
        content = page.getContent();
        metadata = new PageMetadata(page);
    }
}
