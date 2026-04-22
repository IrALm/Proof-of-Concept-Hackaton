package com.example.demo.controller;

import com.example.demo.dto.HotelSearchRequestDto;
import com.example.demo.dto.HotelSearchResultDto;
import com.example.demo.service.HotelSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelSearchController {

    private final HotelSearchService hotelSearchService;

    public HotelSearchController(HotelSearchService hotelSearchService) {
        this.hotelSearchService = hotelSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<List<HotelSearchResultDto>> search(@Valid @RequestBody HotelSearchRequestDto dto) {
        return ResponseEntity.ok(hotelSearchService.search(dto));
    }

    @GetMapping("/home")
    public ResponseEntity<List<HotelSearchResultDto>> home(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(hotelSearchService.homeFeed(limit, offset));
    }

    @GetMapping("/new")
    public ResponseEntity<List<HotelSearchResultDto>> newHotels(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(hotelSearchService.newHotels(limit, offset));
    }

    @GetMapping("/recommended/family")
    public ResponseEntity<List<HotelSearchResultDto>> familyRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(hotelSearchService.familyRecommendations(limit, offset));
    }

    @GetMapping("/recommended/business")
    public ResponseEntity<List<HotelSearchResultDto>> businessRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(hotelSearchService.businessRecommendations(limit, offset));
    }
}
