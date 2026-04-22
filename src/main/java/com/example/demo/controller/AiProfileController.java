package com.example.demo.controller;

import com.example.demo.dto.AiProfileChatRequestDto;
import com.example.demo.dto.AiProfileChatResponseDto;
import com.example.demo.dto.AiProfileSaveRequestDto;
import com.example.demo.dto.AiProfileSaveResponseDto;
import com.example.demo.service.AiProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/profile")
public class AiProfileController {

    private final AiProfileService profileService;

    public AiProfileController(AiProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Starts the profile completion conversation.
     *
     * <pre>
     * POST /ai/profile/start
     * Authorization: Bearer &lt;token&gt;  (optional — enables name personalization)
     * </pre>
     *
     * Returns the IA's opening message, first question, and initial empty preferences.
     */
    @PostMapping("/start")
    public ResponseEntity<AiProfileChatResponseDto> start(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(profileService.start(authorizationHeader));
    }

    /**
     * Processes one turn of the profile conversation.
     *
     * <pre>
     * POST /ai/profile/chat
     * Authorization: Bearer &lt;token&gt;  (optional)
     *
     * {
     *   "message": "J'adore la cuisine italienne et japonaise",
     *   "history": [
     *     { "role": "assistant", "content": "Salut ! Quelles cuisines tu aimes ?" },
     *     { "role": "user",      "content": "J'adore la cuisine italienne et japonaise" }
     *   ]
     * }
     * </pre>
     *
     * <p>When {@code isComplete = true} in the response, the frontend should display
     * a summary and call {@code /save} on user confirmation.
     *
     * <p>Pasting a social profile URL in {@code message} automatically triggers scraping.
     */
    @PostMapping("/chat")
    public ResponseEntity<AiProfileChatResponseDto> chat(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody AiProfileChatRequestDto request
    ) {
        return ResponseEntity.ok(profileService.chat(authorizationHeader, request));
    }

    /**
     * Persists the confirmed preferences to the user's profile.
     *
     * <pre>
     * POST /ai/profile/save
     * Authorization: Bearer &lt;token&gt;  (required)
     *
     * {
     *   "preferences": {
     *     "restaurants": { "cuisines": ["italienne", "japonaise"], ... },
     *     "hotels": { "amenities": ["piscine", "spa"], "budgetLevel": "medium", ... },
     *     "travel": { "favoriteDestinations": ["Rome", "Tokyo"], "travelStyle": "spontané" }
     *   }
     * }
     * </pre>
     */
    @PostMapping("/save")
    public ResponseEntity<AiProfileSaveResponseDto> save(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody AiProfileSaveRequestDto request
    ) {
        return ResponseEntity.ok(profileService.save(authorizationHeader, request));
    }
}