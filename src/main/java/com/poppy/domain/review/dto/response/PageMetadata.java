package com.poppy.domain.review.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class PageMetadata {
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalElements;
    private final boolean first;
    private final boolean last;

    public PageMetadata(Page<?> page) {
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
}
