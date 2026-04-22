package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public record AiHotelSearchRequestDto(
        @Size(min = 3, max = 500, message = "message must contain between 3 and 500 characters")
        String message,
        Integer limit
) {
}
