// controller/RestaurantReservationController.java
package com.example.demo.controller;

import com.example.demo.dto.CreateRestaurantReservationRequest;
import com.example.demo.service.SupabaseDbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("restaurant/reservations")
public class RestaurantReservationController {

    private final SupabaseDbService db;

    public RestaurantReservationController(SupabaseDbService db) {
        this.db = db;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateRestaurantReservationRequest req) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customer_first_name",    req.customerFirstName());
        payload.put("customer_last_name",     req.customerLastName());
        payload.put("customer_email",         req.customerEmail());
        payload.put("customer_phone",         req.customerPhone());
        payload.put("reservation_date",       req.reservationDate());
        payload.put("reservation_time",       req.reservationTime());
        payload.put("duration_minutes",       req.durationMinutes() != null ? req.durationMinutes() : 90);
        payload.put("adults",                 req.adults());
        payload.put("children",               req.children() != null ? req.children() : 0);
        payload.put("babies",                 req.babies() != null ? req.babies() : 0);
        payload.put("table_number",           req.tableNumber());
        payload.put("room_area",              req.roomArea());
        payload.put("meal",                   req.meal());
        payload.put("occasion",               req.occasion());
        payload.put("dietary_restrictions",   req.dietaryRestrictions());
        payload.put("special_requests",       req.specialRequests());
        payload.put("country",                req.country());
        payload.put("reservation_status",     "pending");
        payload.put("reservation_status_date", LocalDate.now().toString());
        payload.values().removeIf(v -> v == null);

        Map<String, Object> created = db.insertRestaurantReservation(payload);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/email")
    public List<Map<String, Object>> getBookingsByEmail(@RequestParam String email) {
        return db.findRestaurantReservationsByEmail(email);
    }

    // 🔍 Vérifier si l’utilisateur peut laisser un avis
    @GetMapping("/can-review/{reservationId}")
    public ResponseEntity<Boolean> canLeaveReview(@PathVariable String reservationId) {

        boolean canReview = db.canUserLeaveRestaurantReview(reservationId);

        return ResponseEntity.ok(canReview);
    }

    // ⭐ Ajouter un avis (si autorisé)
    @PatchMapping("/{reservationId}")
    public ResponseEntity<Map<String, Object>> addReview(
            @PathVariable String reservationId,
            @RequestBody Map<String, String> body
    ) {

        String avis = body.get("avis");

        Map<String, Object> updated =
                db.addRestaurantReview(reservationId, avis);

        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updated);
    }
}