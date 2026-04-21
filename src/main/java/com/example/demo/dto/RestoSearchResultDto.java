package com.example.demo.dto;

public record RestoSearchResultDto(
        Long id,
        String name,
        String location,
        String cuisine,
        String price,
        Double rating,
        Integer reviewCount,
        String photoUrl,
        String award,
        Boolean greenStar,
        Double latitude,
        Double longitude,
        Double score
) {}
