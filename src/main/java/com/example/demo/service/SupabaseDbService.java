package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.example.demo.dto.UserProfileDto;
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
                .defaultHeader("apikey", appProperties.getSupabase().getServiceKey()) // ⚠️ IMPORTANT
                .defaultHeader("Authorization", "Bearer " + appProperties.getSupabase().getServiceKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Prefer", "return=minimal")
                .build();
    }

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

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }
}
