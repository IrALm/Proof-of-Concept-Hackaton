// controller/HotelBookingController.java
package com.example.demo.controller;

import com.example.demo.dto.CreateHotelBookingRequest;
import com.example.demo.service.SupabaseDbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/hotel/bookings")
public class HotelBookingController {

    private final SupabaseDbService db;

    public HotelBookingController(SupabaseDbService db) {
        this.db = db;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateHotelBookingRequest req) {
        LocalDate arrival = LocalDate.parse(req.arrivalDate());

        Map<String, Object> payload = new HashMap<>();
        payload.put("hotel",                       req.hotel());
        payload.put("arrival_date_year",           arrival.getYear());
        payload.put("arrival_date_month",          arrival.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        payload.put("arrival_date_week_number",    arrival.get(WeekFields.ISO.weekOfWeekBasedYear()));
        payload.put("arrival_date_day_of_month",   arrival.getDayOfMonth());
        payload.put("stays_in_weekend_nights",     nz(req.staysInWeekendNights()));
        payload.put("stays_in_week_nights",        nz(req.staysInWeekNights()));
        payload.put("adults",                      nz(req.adults()));
        payload.put("children",                    nz(req.children()));
        payload.put("babies",                      nz(req.babies()));
        payload.put("meal",                        req.meal());
        payload.put("country",                     req.country());
        payload.put("market_segment",              req.marketSegment());
        payload.put("distribution_channel",        req.distributionChannel());
        payload.put("reserved_room_type",          req.reservedRoomType());
        payload.put("deposit_type",                req.depositType());
        payload.put("customer_type",               req.customerType());
        payload.put("adr",                         req.adr());
        payload.put("required_car_parking_spaces", nz(req.requiredCarParkingSpaces()));
        payload.put("total_of_special_requests",   nz(req.totalOfSpecialRequests()));
        payload.put("is_canceled",                 false);
        payload.put("reservation_status",          "Booked");
        payload.put("reservation_status_date",     LocalDate.now().toString());
        payload.put("lead_time",                   (int) ChronoUnit.DAYS.between(LocalDate.now(), arrival));
        payload.values().removeIf(v -> v == null);

        Map<String, Object> created = db.insertHotelBooking(payload);
        return ResponseEntity.status(201).body(created);
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }
}