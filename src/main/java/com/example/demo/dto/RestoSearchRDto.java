package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record RestoSearchRDto(
        String name,
        String location,
        String cuisine,
        Boolean greenStar,
        String award,
        String price,
        @Min(value = -90, message = "Latitude must be >= -90")
        @Max(value = 90, message = "Latitude must be <= 90")
        Double latitude,

        @Min(value = -180, message = "Longitude must be >= -180")
        @Max(value = 180, message = "Longitude must be <= 180")
        Double longitude,

        @PositiveOrZero(message = "Radius must be >= 0")
        Double radius,
        Integer page,
        Integer size,
        Integer limit,
        Integer offset,
        String sortBy,
        String sortDirection
) {}