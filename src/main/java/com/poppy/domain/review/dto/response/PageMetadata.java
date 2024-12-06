package com.poppy.domain.review.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class PageMetadata {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;

    public PageMetadata(Page<?> page) {
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.first = page.isFirst();
        this.last = page.isLast();
    }
}
