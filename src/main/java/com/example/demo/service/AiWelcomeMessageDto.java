package com.example.demo.service;

import java.util.List;

public record AiWelcomeMessageDto(
        String message,
        String mood,
        List<String> suggestedPrompts
) {
}
