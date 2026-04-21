package com.example.demo.service;

import java.util.List;

public record AiSearchNarrativeDto(
        String greeting,
        String assistantMessage,
        List<String> suggestedFollowUps
) {
}
