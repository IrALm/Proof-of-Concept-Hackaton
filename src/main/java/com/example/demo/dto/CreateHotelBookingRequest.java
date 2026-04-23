// dto/CreateHotelBookingRequest.java
package com.example.demo.dto;

public record CreateHotelBookingRequest(
        String hotel,
        String arrivalDate,                // "2026-05-15"
        Integer staysInWeekendNights,
        Integer staysInWeekNights,
        Integer adults,
        Integer children,
        Integer babies,
        String meal,
        String country,
        String marketSegment,
        String distributionChannel,
        String reservedRoomType,
        String depositType,
        String customerType,
        Double adr,
        Integer requiredCarParkingSpaces,
        Integer totalOfSpecialRequests
) {}