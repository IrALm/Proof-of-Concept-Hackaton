package com.example.demo.supabaseAuth;

import com.example.demo.config.AppProperties;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SupabaseAuthService {

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SupabaseAuthService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl(appProperties.getSupabase().getUrl())
                .defaultHeader("apikey", appProperties.getSupabase().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public JsonNode signup(SignupRequest body) {
        String response = restClient.post()
                .uri("/auth/v1/signup")
                .body(Map.of(
                        "email", body.email(),
                        "password", body.password(),
                        "data", Map.of("full_name", body.fullName())
                ))
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Invalid response from Supabase", e);
        }
    }

    public JsonNode login(LoginRequest body) {
        String response = restClient.post()
                .uri("/auth/v1/token?grant_type=password")
                .body(Map.of(
                        "email", body.email(),
                        "password", body.password()
                ))
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Invalid response from Supabase", e);
        }
    }

    public void logout(String token) {
        restClient.post()
                .uri("/auth/v1/logout")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }
}