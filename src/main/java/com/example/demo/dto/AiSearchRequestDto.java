package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AiSearchRequestDto(
        @Size(min = 3, max = 500, message = "message must contain between 3 and 500 characters")
        String message,
        @Min(value = -90, message = "Latitude must be >= -90")
        @Max(value = 90, message = "Latitude must be <= 90")
        Double latitude,
        @Min(value = -180, message = "Longitude must be >= -180")
        @Max(value = 180, message = "Longitude must be <= 180")
        Double longitude,
        @PositiveOrZero(message = "Radius must be >= 0")
        Double radius,
        Integer limit
) {
}
