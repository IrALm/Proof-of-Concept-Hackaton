package com.example.demo.dto;

/**
 * A single turn in the travel planner conversation history.
 * {@code role} is either {@code "user"} or {@code "assistant"}.
 *
 * <p>The frontend must pass the full history with every {@code /ai/travel/plan}
 * request so the backend can resolve intent across multiple turns
 * — exact same pattern as {@code /ai/profile/chat}.
 */
public record AiTravelPlanConversationMessageDto(
        String role,
        String content
) {
}