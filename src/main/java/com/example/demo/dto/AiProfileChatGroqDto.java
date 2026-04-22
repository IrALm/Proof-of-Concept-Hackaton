package com.example.demo.dto;

import java.util.List;

/**
 * Internal DTO representing the structured JSON response produced by Groq
 * during every profile chat turn. Never exposed directly to the client.
 */
public record AiProfileChatGroqDto(

        /** The conversational reply to show to the user. */
        String responseMessage,

        /**
         * {@code true} when the IA judges it has enough data to propose a full profile.
         * Maps directly to {@link com.example.demo.dto.AiProfileChatResponseDto#isComplete()}.
         */
        Boolean isComplete,

        /**
         * 0–100 score reflecting how many preference topics have been covered.
         * {@code isComplete} should be {@code true} when this reaches ≥ 70.
         */
        Integer completionScore,

        /** Preferences extracted so far (partial until isComplete is true). */
        AiProfilePreferencesDto extractedPreferences,

        /** 2–3 quick-reply suggestions. */
        List<String> suggestedReplies

) {
}