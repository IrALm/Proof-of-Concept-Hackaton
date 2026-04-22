package com.example.demo.service;

import com.example.demo.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groq chat client with an automatic <strong>model fallback chain</strong>.
 *
 * <p>Each Groq model has its OWN rate limit bucket (TPM, TPD, RPM, RPD).
 * When the primary model hits a 429, we transparently fall back to the next
 * model in the chain — effectively multiplying the available daily quota by
 * the number of models in the chain.
 *
 * <p>Models that just returned a 429 are temporarily blacklisted (short
 * cooldown parsed from the error message, or a safe default) so we don't
 * hammer them uselessly. Blacklisted models are skipped on subsequent calls
 * until their cooldown expires.
 *
 * <p>Chain order = cheapest/fastest first. We burn the high-volume small
 * model before reaching into the bigger, scarcer ones.
 */
@Service
public class GroqChatService {

    private static final Logger log = LoggerFactory.getLogger(GroqChatService.class);

    // ═══════════════════════════════════════════════════════════════════════
    // MODEL FALLBACK CHAIN
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Ordered from cheapest/highest-quota to top-quality/lower-quota.
    // Each entry has its own rate limit bucket on Groq's side.
    //
    private static final List<String> MODEL_CHAIN = List.of(
            "llama-3.3-70b-versatile",                 // 280 t/s, 300K TPM — quality bump
            "openai/gpt-oss-20b",                      // 1000 t/s, 250K TPM — fastest, native structured
            "llama-3.3-70b-versatile",                 // 280 t/s, 300K TPM — quality bump
            "openai/gpt-oss-120b",                     // 500 t/s, 250K TPM — flagship fallback
            "llama-3.1-8b-instant",                    // 560 t/s, 250K TPM — workhorse for JSON chat
            "meta-llama/llama-4-scout-17b-16e-instruct" // preview, 300K TPM — last-chance fallback
    );

    // Default cooldown applied when we can't parse the "try again in Xs" hint.
    private static final long DEFAULT_COOLDOWN_SECONDS = 15 * 60; // 15 min

    // Parses "try again in 11m1.824s" / "try again in 45.2s" / "try again in 1h2m"
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile(
            "try again in\\s+(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+(?:\\.\\d+)?)s)?",
            Pattern.CASE_INSENSITIVE
    );

    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES & STATE
    // ═══════════════════════════════════════════════════════════════════════

    private final RestClient restClient;

    /** Per-model cooldown: model ID -> Instant until which the model is banned. */
    private final Map<String, Instant> bannedUntil = new ConcurrentHashMap<>();

    public GroqChatService(AppProperties appProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader("Authorization", "Bearer " + appProperties.getGroq().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API (unchanged signatures)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Simple single-turn completion: system prompt + one user message.
     */
    public String completeJson(String systemPrompt, String userPrompt) {
        return executeCompletion(systemPrompt, List.of(
                Map.of("role", "user", "content", userPrompt)
        ));
    }

    /**
     * Multi-turn completion: system prompt + full conversation history.
     */
    public String completeJsonWithHistory(String systemPrompt,
                                          List<Map<String, Object>> conversationMessages) {
        return executeCompletion(systemPrompt, conversationMessages);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CORE EXECUTION WITH FALLBACK
    // ═══════════════════════════════════════════════════════════════════════

    private String executeCompletion(String systemPrompt,
                                     List<Map<String, Object>> userMessages) {

        List<Map<String, Object>> allMessages = new ArrayList<>();
        allMessages.add(Map.of("role", "system", "content", systemPrompt));
        allMessages.addAll(userMessages);

        HttpClientErrorException.TooManyRequests lastRateLimit = null;
        List<String> attempted = new ArrayList<>();

        for (String model : MODEL_CHAIN) {

            // Skip models still in cooldown from a recent 429.
            if (isBanned(model)) {
                log.debug("Skipping model {} (still in cooldown)", model);
                continue;
            }

            attempted.add(model);

            try {
                String result = callGroq(model, allMessages);
                if (!attempted.get(0).equals(model)) {
                    log.info("Groq call succeeded on fallback model '{}' after skipping {}",
                            model, attempted.subList(0, attempted.size() - 1));
                }
                return result;

            } catch (HttpClientErrorException.TooManyRequests e) {
                long cooldownSeconds = parseCooldownSeconds(e.getResponseBodyAsString())
                        .orElse(DEFAULT_COOLDOWN_SECONDS);
                Instant banUntil = Instant.now().plusSeconds(cooldownSeconds);
                bannedUntil.put(model, banUntil);
                lastRateLimit = e;

                log.warn("Rate limit hit on model '{}'. Cooldown: {}s. Falling back...",
                        model, cooldownSeconds);
            }
        }

        // All models in the chain are either exhausted or in cooldown.
        log.error("All Groq models in the fallback chain are exhausted. Attempted: {}", attempted);
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Tous les modèles Groq sont actuellement saturés. Réessaie dans quelques minutes.",
                lastRateLimit
        );
    }

    private String callGroq(String model, List<Map<String, Object>> messages) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.35,
                "response_format", Map.of("type", "json_object"),
                "messages", messages
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
                    HttpStatus.BAD_GATEWAY,
                    "Groq returned an empty response (model=" + model + ")"
            );
        }

        return contentNode.asText();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COOLDOWN MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    private boolean isBanned(String model) {
        Instant until = bannedUntil.get(model);
        if (until == null) return false;
        if (until.isAfter(Instant.now())) return true;
        // Cooldown expired → clear the entry
        bannedUntil.remove(model);
        return false;
    }

    /**
     * Parses Groq's "Please try again in 11m1.824s" hint from a 429 body.
     * Returns total seconds, or empty if the format didn't match.
     */
    private java.util.Optional<Long> parseCooldownSeconds(String errorBody) {
        if (errorBody == null) return java.util.Optional.empty();

        Matcher m = RETRY_AFTER_PATTERN.matcher(errorBody);
        if (!m.find()) return java.util.Optional.empty();

        try {
            long hours   = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
            long minutes = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
            double sec   = m.group(3) != null ? Double.parseDouble(m.group(3)) : 0;

            long total = hours * 3600 + minutes * 60 + (long) Math.ceil(sec);
            // Clamp between 30s (avoid hammering) and 24h (TPD resets daily)
            return java.util.Optional.of(Math.max(30L, Math.min(total, 86400L)));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }
}