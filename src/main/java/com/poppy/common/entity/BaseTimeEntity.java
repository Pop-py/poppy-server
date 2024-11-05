package com.poppy.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Embeddable
@Getter
@AllArgsConstructor
public class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false, name = "create_time")
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(nullable = false, name = "update_time")
    private LocalDateTime updateTime;

    public BaseTimeEntity() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createTime = (this.createTime == null) ? LocalDateTime.now() : this.createTime;
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
