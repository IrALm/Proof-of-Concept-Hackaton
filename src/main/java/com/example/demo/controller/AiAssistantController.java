package com.example.demo.controller;

import com.example.demo.dto.AiSearchRequestDto;
import com.example.demo.dto.AiSearchResponseDto;
import com.example.demo.dto.AiWelcomeResponseDto;
import com.example.demo.service.AiRestaurantAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiAssistantController {

    private final AiRestaurantAssistantService aiRestaurantAssistantService;

    public AiAssistantController(AiRestaurantAssistantService aiRestaurantAssistantService) {
        this.aiRestaurantAssistantService = aiRestaurantAssistantService;
    }

    @GetMapping("/welcome")
    public ResponseEntity<AiWelcomeResponseDto> welcome(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(aiRestaurantAssistantService.welcome(authorization));
    }

    @PostMapping("/search")
    public ResponseEntity<AiSearchResponseDto> search(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiSearchRequestDto request
    ) {
        return ResponseEntity.ok(aiRestaurantAssistantService.search(authorization, request));
    }
}
