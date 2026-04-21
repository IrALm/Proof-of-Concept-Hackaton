package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurants")
@Builder
@Getter
@Setter
public class Restaurant {

    @Id
    private Long id;

    private String name;

    private String address;

    private String location;

    private String price;

    private String cuisine;

    private Double longitude;

    private Double latitude;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "url_michelin")
    private String urlMichelin;

    @Column(name = "website_url")
    private String websiteUrl;

    private String award;

    @Column(name = "green_star")
    private Boolean greenStar;

    @Column(name = "facilities_services", columnDefinition = "TEXT")
    private String facilitiesServices;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "photo_url")
    private String photoUrl;

    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;
}