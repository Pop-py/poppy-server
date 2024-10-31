package com.poppy.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class Images {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_name", nullable = false)
    private String originName;  // 원본 파일 이름

    @Column(name = "stored_name", unique = true, nullable = false)
    private String storedName;  // S3에 저장되는 이름

    @Embedded
    private BaseTimeEntity baseTime;
}
