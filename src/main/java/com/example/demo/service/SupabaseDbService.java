package com.example.demo.service;

import com.example.demo.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
}
