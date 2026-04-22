package com.example.demo.dto;

import java.util.List;

public record AiHotelWelcomeResponseDto(
        String message,
        String mood,
        List<String> suggestedPrompts
) {
}
