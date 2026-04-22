package com.example.demo.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound payload for {@code POST /ai/profile/chat}.
 * The frontend must always send the full conversation {@code history}
 * alongside the current {@code message} — the backend is stateless.
 */
public record AiProfileChatRequestDto(

        @Size(min = 1, max = 1000, message = "message must be between 1 and 1000 characters")
        String message,

        /**
         * Full conversation history in chronological order.
         * May be null or empty on the first turn after /start.
         */
        List<AiProfileConversationMessageDto> history

) {
}