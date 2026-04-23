// dto/CreateRestaurantReservationRequest.java
package com.example.demo.dto;

public record CreateRestaurantReservationRequest(
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        String reservationDate,      // "2026-05-15"
        String reservationTime,      // "20:00"
        Integer durationMinutes,
        Integer adults,
        Integer children,
        Integer babies,
        Integer tableNumber,
        String roomArea,
        String meal,
        String occasion,
        String dietaryRestrictions,
        String specialRequests,
        String country
) {}