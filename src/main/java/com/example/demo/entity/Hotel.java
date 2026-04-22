package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hotels")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {

    @Id
    private Integer id;

    private String name;

    private String country;

    @Column(name = "market_segment")
    private String marketSegment;

    @Column(name = "photo_url")
    private String photoUrl;

    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
