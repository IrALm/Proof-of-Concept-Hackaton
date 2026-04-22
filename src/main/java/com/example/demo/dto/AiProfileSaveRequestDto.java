package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code POST /ai/profile/save}.
 * Called by the frontend once the user confirms the proposed preferences.
 */
public record AiProfileSaveRequestDto(

        @NotNull(message = "preferences must not be null")
        @Valid
        AiProfilePreferencesDto preferences

) {
}