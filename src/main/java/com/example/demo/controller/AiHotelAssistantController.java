package com.example.demo.controller;

import com.example.demo.dto.AiHotelSearchRequestDto;
import com.example.demo.dto.AiHotelSearchResponseDto;
import com.example.demo.dto.AiHotelWelcomeResponseDto;
import com.example.demo.service.AiHotelAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/hotels")
public class AiHotelAssistantController {

    private final AiHotelAssistantService aiHotelAssistantService;

    public AiHotelAssistantController(AiHotelAssistantService aiHotelAssistantService) {
        this.aiHotelAssistantService = aiHotelAssistantService;
    }

    @GetMapping("/welcome")
    public ResponseEntity<AiHotelWelcomeResponseDto> welcome(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(aiHotelAssistantService.welcome(authorization));
    }

    @PostMapping("/search")
    public ResponseEntity<AiHotelSearchResponseDto> search(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiHotelSearchRequestDto request
    ) {
        return ResponseEntity.ok(aiHotelAssistantService.search(authorization, request));
    }
}
