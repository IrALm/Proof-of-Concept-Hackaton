package com.example.demo.dto;

import java.util.List;

/**
 * Response returned by both {@code POST /ai/profile/start} and {@code POST /ai/profile/chat}.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>{@code isComplete = false} → IA is still gathering information; keep calling /chat.</li>
 *   <li>{@code isComplete = true}  → {@code extractedPreferences} is fully populated;
 *       show a summary to the user and call /save on confirmation.</li>
 * </ul>
 */
public record AiProfileChatResponseDto(

        /** The IA's conversational reply shown to the user. */
        String message,

        /**
         * Whether the IA has collected enough information to propose a preferences profile.
         * When true, {@code extractedPreferences} is populated and ready for /save.
         */
        boolean isComplete,

        /**
         * Preferences extracted so far. Partially filled during the conversation,
         * fully populated when {@code isComplete = true}.
         */
        AiProfilePreferencesDto extractedPreferences,

        /**
         * 2–3 quick-reply suggestions the frontend can display as chips/buttons.
         */
        List<String> suggestedReplies

) {
}