package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record HotelSearchRequestDto(
        String name,
        String country,
        String marketSegment,
        @DecimalMin(value = "0.0", message = "minRating must be >= 0")
        @DecimalMax(value = "5.0", message = "minRating must be <= 5")
        Double minRating,
        @PositiveOrZero(message = "maxAdr must be >= 0")
        Double maxAdr,
        String customerType,
        Boolean isRepeatedGuest,
        Boolean familyFriendly,
        Boolean businessFriendly,
        Boolean lowCancellation,
        Boolean newOnly,
        Integer page,
        Integer size,
        Integer limit,
        Integer offset,
        String sortBy,
        String sortDirection
) {
}
