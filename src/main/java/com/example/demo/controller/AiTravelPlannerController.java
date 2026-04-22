package com.example.demo.controller;

import com.example.demo.dto.AiTravelPlanRequestDto;
import com.example.demo.dto.AiTravelPlanResponseDto;
import com.example.demo.service.AiTravelPlannerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/travel")
public class AiTravelPlannerController {

    private final AiTravelPlannerService plannerService;

    public AiTravelPlannerController(AiTravelPlannerService plannerService) {
        this.plannerService = plannerService;
    }

    /**
     * Opens the travel planning conversation.
     *
     * <p>Returns a welcome message + starter quick-reply chips without
     * consuming any Groq tokens (hardcoded, optionally personalized with
     * the user's first name). Mirrors {@code /ai/profile/start}.
     *
     * <pre>
     * POST /ai/travel/start
     * Authorization: Bearer &lt;token&gt;  (optional — enables name personalization)
     * </pre>
     */
    @PostMapping("/start")
    public ResponseEntity<AiTravelPlanResponseDto> start(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(plannerService.start(authorizationHeader));
    }

    /**
     * Processes one turn of the travel planning conversation.
     *
     * <p>Behavior driven by {@code isReadyToPlan} in the response:
     * <ul>
     *   <li>{@code false} → the IA asks a short clarifying question, frontend
     *       keeps chatting and appends to {@code history}.</li>
     *   <li>{@code true}  → the IA has enough info; the backend has already
     *       run the hotel + restaurant search and the narrative step. The
     *       final plan is returned in the SAME response — no extra call
     *       needed.</li>
     * </ul>
     *
     * <pre>
     * POST /ai/travel/plan
     * Authorization: Bearer &lt;token&gt;  (optional — enables personalization)
     *
     * {
     *   "message": "Je veux un week-end romantique a Rome la semaine prochaine",
     *   "history": [
     *     { "role": "assistant", "content": "Salut ! Tu penses a quelle destination ?" },
     *     { "role": "user",      "content": "Je veux un week-end romantique a Rome la semaine prochaine" }
     *   ],
     *   "latitude": 47.2184,
     *   "longitude": -1.5536,
     *   "limit": 5
     * }
     * </pre>
     */
    @PostMapping("/plan")
    public ResponseEntity<AiTravelPlanResponseDto> plan(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody AiTravelPlanRequestDto request
    ) {
        return ResponseEntity.ok(plannerService.plan(authorizationHeader, request));
    }
}