package com.example.demo.dto;

import java.util.List;

public record AiSearchResponseDto(
        String greeting,
        String persona,
        String intentSummary,
        AiSearchIntentDto appliedFilters,
        String assistantMessage,
        List<String> suggestedFollowUps,
        List<RestoSearchResultDto> restaurants
) {
}
