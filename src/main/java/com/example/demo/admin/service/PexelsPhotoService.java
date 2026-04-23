package com.example.demo.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Récupère des URLs de photos depuis l'API Pexels pour enrichir les imports
 * restaurants et hôtels.
 *
 * <p>Le cache en mémoire ({@link ConcurrentHashMap}) évite les appels répétés
 * pour une même clé (cuisine/location ou country/segment), ce qui est critique
 * étant donné la limite de 200 req/h de l'API Pexels gratuite.
 *
 * <p>En cas d'échec (timeout, quota, erreur réseau), un fallback fixe est retourné
 * afin que l'import ne soit jamais bloqué par Pexels.
 */
@Service
public class PexelsPhotoService {

    private static final String PEXELS_BASE_URL = "https://api.pexels.com/v1";

    private static final String RESTAURANT_FALLBACK =
            "https://images.pexels.com/photos/260922/pexels-photo-260922.jpeg";
    private static final String HOTEL_FALLBACK =
            "https://images.pexels.com/photos/258154/pexels-photo-258154.jpeg";

    private final RestClient restClient;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public PexelsPhotoService(@Value("${app.pexels.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(PEXELS_BASE_URL)
                .defaultHeader("Authorization", apiKey)
                .build();
    }

    /**
     * Photo pour un restaurant, basée sur cuisine + localisation.
     */
    public String getRestaurantPhoto(String cuisine, String location) {
        String key = "resto::" + safe(cuisine) + "::" + safe(location);
        String query = (safe(cuisine).isBlank() ? "restaurant" : cuisine)
                + " fine dining " + safe(location);
        return cache.computeIfAbsent(key, k -> fetchPhoto(query, RESTAURANT_FALLBACK));
    }

    /**
     * Photo pour un hôtel, basée sur pays + segment de marché.
     */
    public String getHotelPhoto(String country, String marketSegment) {
        String key = "hotel::" + safe(country) + "::" + safe(marketSegment);
        String query = "luxury hotel "
                + (safe(country).isBlank() ? "europe" : country)
                + " "
                + (safe(marketSegment).isBlank() ? "hotel" : marketSegment);
        return cache.computeIfAbsent(key, k -> fetchPhoto(query, HOTEL_FALLBACK));
    }

    // ─────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────

    private String fetchPhoto(String query, String fallback) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("query", query)
                            .queryParam("per_page", 5)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("photos") && response.get("photos").isArray()
                    && !response.get("photos").isEmpty()) {
                JsonNode firstPhoto = response.get("photos").get(0);
                String url = firstPhoto.path("src").path("large").asText(null);
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        } catch (Exception ignored) {
            // silencieux : on tombe sur le fallback
        }
        return fallback;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /** Vide le cache (utile pour tests ou reset manuel). */
    public void clearCache() {
        cache.clear();
    }
}
