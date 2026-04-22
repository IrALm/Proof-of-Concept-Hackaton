package com.example.demo.dto;

import java.util.List;

/**
 * Unified response for {@code POST /ai/travel/plan}.
 *
 * <p>Exact same pattern as {@code AiProfileChatResponseDto}: the IA decides,
 * turn by turn, whether it has collected enough info. Two possible shapes
 * driven by {@code isReadyToPlan}:
 *
 * <ul>
 *   <li>{@code isReadyToPlan = false} — the IA is still gathering info;
 *       only {@code message} and {@code suggestedReplies} are populated.
 *       The frontend shows the question + chips and keeps calling
 *       {@code /plan} with the updated history.</li>
 *   <li>{@code isReadyToPlan = true}  — the IA has what it needs; the backend
 *       has already run the hotel + restaurant search and the narrative step.
 *       {@code greeting}, {@code summary}, {@code assistantMessage},
 *       {@code hotel}, {@code restaurants} and {@code suggestedReplies} are
 *       populated. No additional call needed.</li>
 * </ul>
 */
public record AiTravelPlanResponseDto(

        // ── Lifecycle flag ───────────────────────────────────────────────────
        /**
         * Whether the IA has collected enough information to trigger a plan.
         * When {@code true}, the plan branch is populated; otherwise only
         * the chat branch is.
         */
        boolean isReadyToPlan,

        // ── Always populated ─────────────────────────────────────────────────
        /** The IA's conversational reply shown to the user. */
        String message,

        /** 2–3 quick-reply suggestions the frontend can display as chips. */
        List<String> suggestedReplies,

        // ── Plan branch (populated only when isReadyToPlan = true) ───────────
        String greeting,
        String persona,
        String intentSummary,
        String summary,
        String assistantMessage,
        HotelSearchResultDto hotel,
        List<RestoSearchResultDto> restaurants

) {

    // ── Static factory helpers ───────────────────────────────────────────────

    /**
     * Chat turn when the IA still needs more info from the user.
     */
    public static AiTravelPlanResponseDto chatTurn(String message, List<String> suggestedReplies) {
        return new AiTravelPlanResponseDto(
                false,
                message,
                suggestedReplies,
                null, null, null, null, null, null, null
        );
    }

    /**
     * Final response once the IA flipped {@code isReadyToPlan} to true and the
     * backend ran the hotel + restaurant search.
     */
    public static AiTravelPlanResponseDto plan(
            String message,
            List<String> suggestedReplies,
            String greeting,
            String persona,
            String intentSummary,
            String summary,
            String assistantMessage,
            HotelSearchResultDto hotel,
            List<RestoSearchResultDto> restaurants
    ) {
        return new AiTravelPlanResponseDto(
                true,
                message,
                suggestedReplies,
                greeting,
                persona,
                intentSummary,
                summary,
                assistantMessage,
                hotel,
                restaurants
        );
    }
}