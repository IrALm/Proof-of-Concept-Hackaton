package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
public record AiSearchIntentDto(
        String persona,
        String intentSummary,
        String responseTone,
        String name,
        String location,
        String cuisine,
        Boolean greenStar,
        String award,
        String price,
        Double latitude,
        Double longitude,
        Boolean newOnly,
        Double radius,
        Integer limit,
        String sortBy,
        String sortDirection
) {

    public RestoSearchRDto toSearchDto() {
        return RestoSearchRDto.builder()
                .name(blankToNull(name))
                .location(blankToNull(location))
                .cuisine(blankToNull(cuisine))
                .greenStar(greenStar)
                .award(blankToNull(award))
                .price(blankToNull(price))
                .latitude(latitude)
                .longitude(longitude)
                .newOnly(newOnly)
                .radius(radius)
                .limit(limit)
                .sortBy(blankToNull(sortBy))
                .sortDirection(blankToNull(sortDirection))
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
