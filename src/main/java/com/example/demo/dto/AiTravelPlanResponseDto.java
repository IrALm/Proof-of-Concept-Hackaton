package com.example.demo.dto;

import java.util.List;

/**
 * Unified response for the travel planner endpoint.
 *
 * <p>Two possible states driven by {@code status}:
 * <ul>
 *   <li>{@code "clarification_needed"} — the prompt was too vague; only
 *       {@code message} and {@code missingFields} are populated.</li>
 *   <li>{@code "plan_ready"} — a complete trip proposal; {@code summary},
 *       {@code greeting}, {@code assistantMessage}, {@code suggestedFollowUps},
 *       {@code hotel} and {@code restaurants} are populated.</li>
 * </ul>
 */
public record AiTravelPlanResponseDto(

        // ── State ────────────────────────────────────────────────────────────
        /**
         * Either {@code "clarification_needed"} or {@code "plan_ready"}.
         */
        String status,

        // ── Clarification branch ─────────────────────────────────────────────
        /**
         * Conversational question returned to the user when clarification is needed.
         */
        String message,

        /**
         * Fields that were detected as missing (e.g. {@code ["destination"]}).
         */
        List<String> missingFields,

        // ── Plan branch ──────────────────────────────────────────────────────
        String greeting,
        String persona,
        String intentSummary,
        String summary,
        String assistantMessage,
        List<String> suggestedFollowUps,

        HotelSearchResultDto hotel,
        List<RestoSearchResultDto> restaurants

) {

    // ── Static factory helpers ───────────────────────────────────────────────

    public static AiTravelPlanResponseDto clarification(String question, List<String> missingFields) {
        return new AiTravelPlanResponseDto(
                "clarification_needed",
                question,
                missingFields,
                null, null, null, null, null, null,
                null, null
        );
    }

    public static AiTravelPlanResponseDto plan(
            String greeting,
            String persona,
            String intentSummary,
            String summary,
            String assistantMessage,
            List<String> suggestedFollowUps,
            HotelSearchResultDto hotel,
            List<RestoSearchResultDto> restaurants
    ) {
        return new AiTravelPlanResponseDto(
                "plan_ready",
                null, null,
                greeting,
                persona,
                intentSummary,
                summary,
                assistantMessage,
                suggestedFollowUps,
                hotel,
                restaurants
        );
    }
}