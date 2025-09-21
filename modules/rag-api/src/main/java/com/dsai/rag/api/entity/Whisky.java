package com.dsai.rag.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "whiskies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Whisky {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    private String region; // 스코틀랜드(스파이사이드, 아일라 등), 아일랜드, 미국, 일본 등

    @Column(nullable = false)
    private String type; // Single Malt, Blended, Bourbon, Rye 등

    private Integer age; // 숙성 년수

    private Double abv; // 알코올 도수

    @Column(length = 2000)
    private String description; // 상세 설명

    @Column(length = 1000)
    private String tastingNotes; // 테이스팅 노트

    private String flavorProfile; // peaty, smoky, sweet, fruity, spicy 등

    private Integer priceRange; // 1-5 (저가부터 프리미엄)

    private Double rating; // 평점

    @Column(length = 1000)
    private String foodPairing; // 페어링 음식

    private String occasion; // 추천 상황 (입문용, 선물용, 특별한날 등)

    private Boolean isAvailable;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}