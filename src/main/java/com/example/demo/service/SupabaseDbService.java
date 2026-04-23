package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.dto.AiProfilePreferencesDto;
import com.example.demo.dto.UserProfileDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseDbService {

    private final AppProperties appProperties;
    private final RestClient restClient;

    public SupabaseDbService(AppProperties appProperties) {
        this.appProperties = appProperties;

        this.restClient = RestClient.builder()
                .baseUrl(appProperties.getSupabase().getUrl() + "/rest/v1")
                .defaultHeader("apikey", appProperties.getSupabase().getServiceKey())
                .defaultHeader("Authorization", "Bearer " + appProperties.getSupabase().getServiceKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Prefer", "return=minimal")
                .build();
    }

    // ═════════════════════════════════════════════════════════════════
    // USERS
    // ═════════════════════════════════════════════════════════════════

    public void insertUser(String id, String email, String fullName) {
        restClient.post()
                .uri("/users")
                .body(Map.of(
                        "id", id,
                        "email", email,
                        "full_name", fullName != null ? fullName : ""
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public UserProfileDto findUserByToken(String accessToken) {

        String supabaseAnonKey = "sb_publishable_hOvgbgZG6112bFo1vmAA1Q_zAq5F5Dj";

        // ===== 1. AUTH =====
        RestClient authClient = RestClient.builder()
                .baseUrl("https://pauhbpfnpelchbkawjhh.supabase.co/auth/v1")
                .build();

        Map<String, Object> user = authClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("apikey", supabaseAnonKey)
                .retrieve()
                .body(Map.class);

        if (user == null) {
            return null;
        }

        String id = valueAsString(user.get("id"));
        String email = valueAsString(user.get("email"));

        Map<String, Object> userMetadata =
                (Map<String, Object>) user.get("user_metadata");

        String fullName = userMetadata != null
                ? valueAsString(userMetadata.get("full_name"))
                : null;

        // ===== 2. DB (preferences depuis table users) =====
        RestClient dbClient = RestClient.builder()
                .baseUrl("https://pauhbpfnpelchbkawjhh.supabase.co/rest/v1")
                .build();

        List<Map<String, Object>> rows = dbClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("id", "eq." + id)
                        .queryParam("select", "preferences")
                        .build())
                .header("apikey", supabaseAnonKey)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(List.class);

// 🔍 DEBUG RAW
        System.out.println("RAW DB RESPONSE: " + rows);

        Map<String, Object> preferences = null;

        if (rows != null && !rows.isEmpty()) {
            preferences = (Map<String, Object>) rows.get(0).get("preferences");
        }

        return new UserProfileDto(
                id,
                email,
                fullName,
                preferences
        );
    }

    public void updatePreferences(String userId, AiProfilePreferencesDto preferences) {
        restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("id", "eq." + userId)
                        .build())
                .body(Map.of("preferences", preferences))
                .retrieve()
                .toBodilessEntity();
    }

    public UserProfileDto findAuthUser(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        JsonNode user = RestClient.builder()
                .baseUrl(appProperties.getSupabase().getUrl())
                .defaultHeader("apikey", appProperties.getSupabase().getAnonKey())
                .build()
                .get()
                .uri("/auth/v1/user")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(JsonNode.class);

        if (user == null || user.isMissingNode()) {
            return null;
        }

        JsonNode userMetadata = user.path("user_metadata");

        return new UserProfileDto(
                user.path("id").asText(null),
                user.path("email").asText(null),
                firstNonBlank(
                        user.path("full_name").asText(null),
                        user.path("name").asText(null),
                        userMetadata.path("full_name").asText(null),
                        userMetadata.path("name").asText(null),
                        userMetadata.path("display_name").asText(null)
                ), null
                );
    }

    // ═════════════════════════════════════════════════════════════════
    // RESTAURANTS (back-office)
    // ═════════════════════════════════════════════════════════════════

    public void insertRestaurantsBatch(List<Map<String, Object>> batch) {
        restClient.post()
                .uri("/restaurants")
                .body(batch)
                .retrieve()
                .toBodilessEntity();
    }

    // ═════════════════════════════════════════════════════════════════
    // HOTELS (back-office)
    // ═════════════════════════════════════════════════════════════════

    public void insertHotel(Map<String, Object> hotel) {
        restClient.post()
                .uri("/hotels")
                .body(hotel)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Liste des hôtels pour alimenter le dropdown du formulaire de booking.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findAllHotels() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/hotels")
                        .queryParam("select", "id,name,country,market_segment")
                        .queryParam("order", "name.asc")
                        .build())
                .retrieve()
                .body(List.class);
    }

    // ═════════════════════════════════════════════════════════════════
    // HOTEL_BOOKINGS (back-office)
    // ═════════════════════════════════════════════════════════════════


    @SuppressWarnings("unchecked")
    public Map<String, Object> insertHotelBooking(Map<String, Object> booking) {
        List<Map<String, Object>> result = restClient.post()
                .uri("/hotel_bookings")
                .header("Prefer", "return=representation")
                .body(List.of(booking))
                .retrieve()
                .body(List.class);

        return (result == null || result.isEmpty()) ? null : result.get(0);
    }

    // ═════════════════════════════════════════════════════════════════
    // RESTAURANT_RESERVATIONS
    // ═════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, Object> insertRestaurantReservation(Map<String, Object> reservation) {
        List<Map<String, Object>> result = restClient.post()
                .uri("/restaurant_reservations")
                .header("Prefer", "return=representation")
                .body(List.of(reservation))
                .retrieve()
                .body(List.class);

        return (result == null || result.isEmpty()) ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findRestaurantReservationsByEmail(String email) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/restaurant_reservations")
                        .queryParam("customer_email", "eq." + email)
                        .queryParam("order", "created_at.desc")
                        .build())
                .retrieve()
                .body(List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findHotelBookingsByEmail(String email) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/hotel_bookings")
                        .queryParam("email", "eq." + email)
                        .build())
                .retrieve()
                .body(List.class);
    }

    @SuppressWarnings("unchecked")
    public boolean canUserLeaveRestaurantReview(String reservationId) {

        // 1. récupérer la réservation
        List<Map<String, Object>> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/restaurant_reservations")
                        .queryParam("id", "eq." + reservationId)
                        .queryParam("select", "reservation_date")
                        .build())
                .retrieve()
                .body(List.class);

        if (result == null || result.isEmpty()) {
            return false;
        }

        // 2. extraire la date
        String dateStr = (String) result.get(0).get("reservation_date");

        if (dateStr == null) {
            return false;
        }

        // 3. comparer avec aujourd'hui
        LocalDate reservationDate = LocalDate.parse(dateStr);
        LocalDate today = LocalDate.now();

        // 4. règle métier : avis autorisé seulement si date passée
        return reservationDate.isBefore(today);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> addRestaurantReview(String reservationId, String avis) {

        if (!canUserLeaveRestaurantReview(reservationId)) {
            throw new IllegalStateException("Impossible de laisser un avis avant la date de réservation");
        }

        List<Map<String, Object>> result = restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/restaurant_reservations")
                        .queryParam("id", "eq." + reservationId)
                        .build())
                .header("Prefer", "return=representation") // 🔥 IMPORTANT
                .body(Map.of("avis", avis))
                .retrieve()
                .body(List.class);

        return (result == null || result.isEmpty()) ? null : result.get(0);
    }

    @SuppressWarnings("unchecked")
    public boolean canUserLeaveHotelReview(String bookingId) {

        // 1. récupérer la réservation hôtel
        List<Map<String, Object>> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/hotel_bookings")
                        .queryParam("id", "eq." + bookingId)
                        .queryParam("select", "reservation_status_date")
                        .build())
                .retrieve()
                .body(List.class);

        if (result == null || result.isEmpty()) {
            return false;
        }

        String dateStr = (String) result.get(0).get("reservation_status_date");

        if (dateStr == null) {
            return false;
        }

        LocalDate reservationDate = LocalDate.parse(dateStr);
        LocalDate today = LocalDate.now();

        return reservationDate.isBefore(today);
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> addHotelReview(String bookingId, String avis) {

        if (!canUserLeaveHotelReview(bookingId)) {
            throw new IllegalStateException("Impossible de laisser un avis avant la fin de la réservation");
        }

        List<Map<String, Object>> result = restClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/hotel_bookings")
                        .queryParam("id", "eq." + bookingId)
                        .build())
                .header("Prefer", "return=representation") // 🔥 indispensable
                .body(Map.of("avis", avis))
                .retrieve()
                .body(List.class);

        return (result == null || result.isEmpty()) ? null : result.get(0);
    }

    // ═════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }
}
