package com.poppy.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Embeddable
public class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false, name = "create_time")
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(nullable = false, name = "update_time")
    private LocalDateTime updateTime;
}
