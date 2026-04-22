package com.example.demo.dto;

import com.example.demo.dto.HotelSearchRequestDto;
import com.example.demo.dto.RestoSearchRDto;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * Structured representation of what the user wants for their trip.
 * Produced by the first Groq call (intent-parsing step).
 */
@Builder(toBuilder = true)
@With
public record AiTravelPlanIntentDto(

        // ── Orchestration metadata ──────────────────────────────────────────
        String persona,
        String intentSummary,
        String responseTone,

        // ── Destination ─────────────────────────────────────────────────────
        String destinationCountry,
        String destinationCity,

        // ── Trip context ─────────────────────────────────────────────────────
        String startDateHint,
        String endDateHint,

        /**
         * One of: leisure | business | romantic | family | adventure
         */
        String tripType,

        /**
         * One of: low | medium | high | luxury
         */
        String budgetLevel,
        Integer partySize,

        // ── What the user wants ──────────────────────────────────────────────
        Boolean wantsHotel,
        Boolean wantsRestaurants,

        // ── Clarification ────────────────────────────────────────────────────
        Boolean needsClarification,
        List<String> missingFields,
        String clarifyingQuestion,

        // ── Search tuning ────────────────────────────────────────────────────
        Integer hotelLimit,
        Integer restoLimit

) {

    // ── Factory helpers ──────────────────────────────────────────────────────

    /**
     * Converts the resolved intent into a hotel search request.
     * Uses country as the primary geographic filter; name search is left null
     * so the engine returns a ranked list for the destination.
     */
    public HotelSearchRequestDto toHotelSearchDto() {
        String sortBy = resolveSortBy();
        return HotelSearchRequestDto.builder()
                .country(destinationCountry)
                .familyFriendly(isTripType("family") ? true : null)
                .businessFriendly(isTripType("business") ? true : null)
                .sortBy(sortBy)
                .limit(safeHotelLimit())
                .offset(0)
                .build();
    }

    /**
     * Converts the resolved intent into a restaurant search request.
     * Uses destinationCity as the location filter when available.
     */
    public RestoSearchRDto toRestoSearchDto() {
        return RestoSearchRDto.builder()
                .location(destinationCity != null ? destinationCity : destinationCountry)
                .price(resolvePriceTier())
                .limit(safeRestoLimit())
                .offset(0)
                .build();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private boolean isTripType(String type) {
        return type != null && type.equalsIgnoreCase(tripType);
    }

    private String resolveSortBy() {
        if (isTripType("family")) return "family";
        if (isTripType("business")) return "business";
        return "score";
    }

    /**
     * Maps the AI-generated budget level to a Michelin price tier (€ … €€€€).
     */
    private String resolvePriceTier() {
        if (budgetLevel == null) return null;
        return switch (budgetLevel.trim().toLowerCase()) {
            case "low" -> "€";
            case "medium" -> "€€";
            case "high" -> "€€€";
            case "luxury" -> "€€€€";
            default -> null;
        };
    }

    private int safeHotelLimit() {
        if (hotelLimit == null || hotelLimit < 1 || hotelLimit > 5) return 1;
        return hotelLimit;
    }

    private int safeRestoLimit() {
        if (restoLimit == null || restoLimit < 1 || restoLimit > 10) return 5;
        return restoLimit;
    }
}