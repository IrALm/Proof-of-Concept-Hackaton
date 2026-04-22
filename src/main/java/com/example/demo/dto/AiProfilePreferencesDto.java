package com.example.demo.dto;

import java.util.List;

/**
 * Structured user preferences stored in the {@code preferences} JSONB column.
 * All fields are nullable so partial saves are safe at any stage of the conversation.
 */
public record AiProfilePreferencesDto(

        RestaurantPreferences restaurants,
        HotelPreferences hotels,
        TravelPreferences travel

) {

    public record RestaurantPreferences(
            List<String> cuisines,
            List<String> priceRange,
            List<String> atmosphere,
            List<String> dietaryRestrictions
    ) {
    }

    public record HotelPreferences(
            List<String> amenities,
            List<String> tripTypes,
            /**
             * One of: low | medium | high | luxury
             */
            String budgetLevel,
            List<String> preferredSegments
    ) {
    }

    public record TravelPreferences(
            List<String> favoriteDestinations,
            /**
             * One of: spontané | planifié | aventurier | confort
             */
            String travelStyle
    ) {
    }
}