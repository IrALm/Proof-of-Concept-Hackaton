package com.example.demo.service;

import java.util.List;

public record AiHotelWelcomeMessageDto(
        String message,
        String mood,
        List<String> suggestedPrompts
) {
}
