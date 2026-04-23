// controller/AiBookingController.java
package com.example.demo.controller;

import com.example.demo.dto.AiBookingRequestDto;
import com.example.demo.dto.AiBookingResponseDto;
import com.example.demo.service.AiHotelBookingService;
import com.example.demo.service.AiRestaurantBookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiBookingController {

    private final AiRestaurantBookingService restaurantService;
    private final AiHotelBookingService hotelService;

    public AiBookingController(AiRestaurantBookingService r, AiHotelBookingService h) {
        this.restaurantService = r;
        this.hotelService = h;
    }

    @PostMapping("/restaurant/book")
    public AiBookingResponseDto bookRestaurant(@Valid @RequestBody AiBookingRequestDto req) {
        return restaurantService.handle(req);
    }

    @PostMapping("/hotel/book")
    public AiBookingResponseDto bookHotel(@Valid @RequestBody AiBookingRequestDto req) {
        return hotelService.handle(req);
    }
}