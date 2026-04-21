package com.example.demo.dto;

import java.util.List;

public record AiWelcomeResponseDto(
        String message,
        String mood,
        List<String> suggestedPrompts
) {
}
