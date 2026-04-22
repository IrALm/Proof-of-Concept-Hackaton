package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
public record AiHotelSearchIntentDto(
        String persona,
        String intentSummary,
        String responseTone,
        String name,
        String country,
        String marketSegment,
        Double minRating,
        Double maxAdr,
        String customerType,
        Boolean isRepeatedGuest,
        Boolean familyFriendly,
        Boolean businessFriendly,
        Boolean lowCancellation,
        Boolean newOnly,
        Integer limit,
        String sortBy,
        String sortDirection
) {

    public HotelSearchRequestDto toSearchDto() {
        return HotelSearchRequestDto.builder()
                .name(blankToNull(name))
                .country(blankToNull(country))
                .marketSegment(blankToNull(marketSegment))
                .minRating(minRating)
                .maxAdr(maxAdr)
                .customerType(blankToNull(customerType))
                .isRepeatedGuest(isRepeatedGuest)
                .familyFriendly(familyFriendly)
                .businessFriendly(businessFriendly)
                .lowCancellation(lowCancellation)
                .newOnly(newOnly)
                .limit(limit)
                .sortBy(blankToNull(sortBy))
                .sortDirection(blankToNull(sortDirection))
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
