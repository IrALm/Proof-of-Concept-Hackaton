package com.example.demo.dto;

import java.util.List;

public record AiHotelSearchResponseDto(
        String greeting,
        String persona,
        String intentSummary,
        AiHotelSearchIntentDto appliedFilters,
        String assistantMessage,
        List<String> suggestedFollowUps,
        List<HotelSearchResultDto> hotels
) {
}
