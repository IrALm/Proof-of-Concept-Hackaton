package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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

    // ─────────────────────────────────────────────────────────────────────
    // SINGLE-TURN  (existing behaviour — unchanged)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Simple single-turn completion: system prompt + one user message.
     * Used by all existing assistant services.
     */
    public String completeJson(String systemPrompt, String userPrompt) {
        return executeCompletion(systemPrompt, List.of(
                Map.of("role", "user", "content", userPrompt)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    // MULTI-TURN  (new — required by AiProfileService)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Multi-turn completion: system prompt + full conversation history.
     * Each entry in {@code conversationMessages} must have {@code role} and {@code content}.
     * The last entry should be the current user message.
     *
     * <pre>
     * groqChatService.completeJsonWithHistory(SYSTEM_PROMPT, List.of(
     *   Map.of("role", "assistant", "content", "Quelles cuisines tu aimes ?"),
     *   Map.of("role", "user",      "content", "J'aime l'italienne et le japonais"),
     *   Map.of("role", "assistant", "content", "Super ! Et pour les hôtels ?"),
     *   Map.of("role", "user",      "content", "Je veux une piscine")
     * ));
     * </pre>
     */
    public String completeJsonWithHistory(String systemPrompt,
                                          List<Map<String, Object>> conversationMessages) {
        return executeCompletion(systemPrompt, conversationMessages);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SHARED HTTP EXECUTION
    // ─────────────────────────────────────────────────────────────────────

    private String executeCompletion(String systemPrompt,
                                     List<Map<String, Object>> userMessages) {

        List<Map<String, Object>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));
        allMessages.addAll(userMessages);

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.35,
                "response_format", Map.of("type", "json_object"),
                "messages", allMessages
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