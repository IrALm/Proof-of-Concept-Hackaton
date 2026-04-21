package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class GroqChatService {

    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";

    private final RestClient restClient;
    private final String model;

    public GroqChatService(AppProperties appProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + appProperties.getGroq().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = DEFAULT_MODEL;
    }

    public String completeJson(String systemPrompt, String userPrompt) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.35,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        JsonNode contentNode = response
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Groq returned an empty response"
            );
        }

        return contentNode.asText();
    }
}
