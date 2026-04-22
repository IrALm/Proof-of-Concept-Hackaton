package com.example.demo.service;

import com.example.demo.dto.AiTravelPlanIntentDto;

import java.util.List;

/**
 * Internal DTO representing the structured JSON response produced by Groq
 * during every travel planner chat turn. Never exposed directly to the client.
 *
 * <p>Mirrors the exact same pattern as {@code AiProfileChatGroqDto}:
 * <ul>
 *   <li>{@code isReadyToPlan = false} → Groq is still gathering info → we
 *       return the question to the user and keep chatting.</li>
 *   <li>{@code isReadyToPlan = true}  → Groq has everything it needs →
 *       backend triggers the hotel + restaurant search and the narrative step.</li>
 * </ul>
 */
public record AiTravelPlanChatGroqDto(

        /** The conversational reply to show to the user (question or confirmation). */
        String responseMessage,

        /**
         * {@code true} when the IA judges it has enough data to trigger a full plan.
         * This is the EXACT equivalent of {@code isComplete} in the profile flow —
         * when {@code true}, the backend triggers the search + narrative step.
         */
        Boolean isReadyToPlan,

        /**
         * 0–100 score reflecting how many planning fields are filled.
         * {@code isReadyToPlan} should flip to {@code true} when this reaches ≥ 70
         * AND a destination is present.
         */
        Integer completionScore,

        /** Intent extracted so far (partial until isReadyToPlan is true). */
        AiTravelPlanIntentDto collectedIntent,

        /** 2–3 quick-reply suggestions. */
        List<String> suggestedReplies

) {
}