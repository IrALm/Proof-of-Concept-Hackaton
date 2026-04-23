// dto/AiBookingResponseDto.java
package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public record AiBookingResponseDto(
        String assistantMessage,
        Map<String, Object> collected,           // état mis à jour à renvoyer au prochain tour
        List<String> missingRequired,
        List<String> suggestedFollowUps,
        String status,                           // "collecting" | "awaiting_confirmation" | "confirmed"
        String bookingId,                        // non-null quand status = "confirmed"
        String summary                           // récap avant confirmation
) {}