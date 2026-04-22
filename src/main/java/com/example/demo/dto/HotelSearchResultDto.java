package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record HotelSearchResultDto(
        Integer id,
        String name,
        String country,
        String marketSegment,
        String photoUrl,
        Double rating,
        Integer reviewCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        Double avgAdr,
        Integer bookingCount,
        Double cancellationRate,
        Double repeatedGuestRate,
        Double familyScore,
        Double businessScore,
        Double avgSpecialRequests,
        Double score
) {
}
