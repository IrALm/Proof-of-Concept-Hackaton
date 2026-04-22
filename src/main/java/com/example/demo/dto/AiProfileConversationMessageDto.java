package com.example.demo.dto;

/**
 * A single turn in the profile conversation history.
 * {@code role} is either {@code "user"} or {@code "assistant"}.
 * The frontend must pass the full history with every /chat request.
 */
public record AiProfileConversationMessageDto(
        String role,
        String content
) {
}