package com.example.demo.service;

import java.util.List;

public record AiHotelSearchNarrativeDto(
        String greeting,
        String assistantMessage,
        List<String> suggestedFollowUps
) {
}
