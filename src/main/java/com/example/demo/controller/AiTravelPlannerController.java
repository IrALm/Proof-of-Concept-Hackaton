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
     * Natural-language travel planning endpoint.
     *
     * <p>Accepts a freeform user message and optional geolocation context.
     * Returns either a clarification question or a full trip proposal
     * (hotel + restaurants + narrative).
     *
     * <pre>
     * POST /ai/travel/plan
     * Authorization: Bearer &lt;token&gt;  (optional — enables personalization)
     *
     * {
     *   "message": "Je veux un week-end romantique a Rome la semaine prochaine",
     *   "latitude": 48.8566,
     *   "longitude": 2.3522,
     *   "limit": 5
     * }
     * </pre>
     */
    @PostMapping("/plan")
    public ResponseEntity<AiTravelPlanResponseDto> plan(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody AiTravelPlanRequestDto request
    ) {
        AiTravelPlanResponseDto response = plannerService.plan(authorizationHeader, request);
        return ResponseEntity.ok(response);
    }
}