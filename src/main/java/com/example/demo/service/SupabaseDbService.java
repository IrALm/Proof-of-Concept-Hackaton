package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.dto.AiProfilePreferencesDto;
import com.example.demo.dto.UserProfileDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    public UserProfileDto findUserById(String id) {
        List<Map> rows = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("id", "eq." + id)
                        .queryParam("select", "id,email,full_name")
                        .build())
                .retrieve()
                .body(List.class);

        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Map firstRow = rows.get(0);

        return new UserProfileDto(
                valueAsString(firstRow.get("id")),
                valueAsString(firstRow.get("email")),
                valueAsString(firstRow.get("full_name"))
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
                )
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


    public void insertHotelBooking(Map<String, Object> booking) {
        restClient.post()
                .uri("/hotel_bookings")
                .body(List.of(booking))
                .retrieve()
                .toBodilessEntity();
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
