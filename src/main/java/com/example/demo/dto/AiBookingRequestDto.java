// dto/AiBookingRequestDto.java
package com.example.demo.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record AiBookingRequestDto(
        @Size(min = 1, max = 500) String message,
        List<AiChatMessage> conversation,     // historique : {role, content}
        Map<String, Object> collected,        // état partiel déjà extrait
        Map<String, Object> context           // pour hôtel : {hotelName, roomType, adr}
) {}