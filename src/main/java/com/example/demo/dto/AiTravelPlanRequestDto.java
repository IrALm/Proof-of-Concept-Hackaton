package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound payload for {@code POST /ai/travel/plan}.
 *
 * <p>EXACT same pattern as {@code AiProfileChatRequestDto}: the frontend sends
 * the current {@code message} plus the full conversation {@code history}.
 * The backend is stateless — the conversation is replayed at each call.
 *
 * <p>Flow:
 * <ul>
 *   <li>The IA decides turn by turn if it has enough info
 *       ({@code isReadyToPlan} in the response).</li>
 *   <li>As long as it's not ready, the response is a conversational question.
 *       The frontend keeps the chat going and appends turns to the history.</li>
 *   <li>As soon as it flips to {@code true}, the backend runs the hotel +
 *       restaurant search and returns the full plan in the SAME response.</li>
 * </ul>
 */
public record AiTravelPlanRequestDto(

        @Size(min = 1, max = 1000, message = "message must contain between 1 and 1000 characters")
        String message,

        /**
         * Full conversation history in chronological order.
         * May be null or empty on the very first turn.
         */
        List<AiTravelPlanConversationMessageDto> history,

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