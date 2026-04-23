// service/AiRestaurantBookingService.java
package com.example.demo.service;

import com.example.demo.dto.AiBookingRequestDto;
import com.example.demo.dto.AiBookingResponseDto;
import com.example.demo.dto.AiChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class AiRestaurantBookingService {

    private static final List<String> REQUIRED = List.of(
            "customer_first_name", "customer_last_name", "customer_email", "customer_phone",
            "reservation_date", "reservation_time", "adults"
    );

    private final GroqChatService groq;
    private final SupabaseDbService db;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiRestaurantBookingService(GroqChatService groq, SupabaseDbService db) {
        this.groq = groq;
        this.db = db;
    }

    public AiBookingResponseDto handle(AiBookingRequestDto req) {
        String systemPrompt = buildSystemPrompt();

        // Construction de l'historique pour Groq
        List<Map<String, Object>> messages = new ArrayList<>();
        if (req.conversation() != null) {
            for (AiChatMessage m : req.conversation()) {
                messages.add(Map.of("role", m.role(), "content", m.content()));
            }
        }
        // État courant envoyé à l'IA pour fusion
        String stateAsJson = safeJson(req.collected() == null ? Map.of() : req.collected());
        messages.add(Map.of("role", "user", "content",
                "État actuel: " + stateAsJson + "\nNouveau message: " + req.message()));

        String raw = groq.completeJsonWithHistory(systemPrompt, messages);
        JsonNode json = parse(raw);

        Map<String, Object> collected = toMap(json.path("collected"));
        List<String> missing = toStringList(json.path("missingRequired"));
        String assistantMessage = json.path("assistantMessage").asText("");
        List<String> followUps = toStringList(json.path("suggestedFollowUps"));
        String status = json.path("status").asText("collecting");
        String summary = json.path("summary").asText(null);

        String bookingId = null;

        // Si l'IA estime que l'utilisateur a confirmé ET qu'il ne manque rien → insert
        if ("confirmed".equals(status) && missing.isEmpty()) {
            Map<String, Object> payload = new HashMap<>(collected);
            payload.putIfAbsent("reservation_status", "pending");
            payload.putIfAbsent("reservation_status_date", LocalDate.now().toString());
            payload.putIfAbsent("duration_minutes", 90);
            payload.values().removeIf(Objects::isNull);

            db.insertRestaurantReservation(payload);
            bookingId = "ok";  // ton insert actuel ne renvoie pas l'id ; voir note plus bas
        } else if ("confirmed".equals(status)) {
            // Incohérence IA : on bascule en awaiting
            status = "awaiting_confirmation";
        }

        return new AiBookingResponseDto(
                assistantMessage, collected, missing, followUps, status, bookingId, summary
        );
    }

    private String buildSystemPrompt() {
        return """
          Tu es un assistant de réservation pour un restaurant.
          Ton rôle : collecter le MINIMUM d'infos pour réserver, en français, ton chaleureux et concis.

          Champs REQUIS (snake_case) :
          - customer_first_name, customer_last_name
          - customer_email, customer_phone
          - reservation_date (format YYYY-MM-DD)
          - reservation_time (format HH:MM sur 24h)
          - adults (entier ≥ 1)

          Champs OPTIONNELS : children, babies, meal (lunch/dinner), occasion, special_requests.

          Règles :
          - Aujourd'hui : """ + LocalDate.now() + """
           . Convertis "ce soir", "demain", "samedi" en date absolue.
          - Demande UN SEUL champ manquant à la fois, dans l'ordre :
            date → heure → nombre de personnes → prénom → nom → email → téléphone.
          - Ne fabrique jamais d'information. Si tu as un doute, demande.
          - Quand tout est rempli, passe en status "awaiting_confirmation" et fais un récap
            dans "summary", puis demande "Je confirme ?".
          - Si l'utilisateur confirme explicitement (oui, confirme, valide, go, ok), passe en "confirmed".
          - Si l'utilisateur annule (non, annule, attends), reviens à "collecting".

          Tu DOIS répondre UNIQUEMENT par un JSON valide avec ce schéma exact :
          {
            "collected": { /* tous les champs connus à ce stade, fusion de l'état + nouveau message */ },
            "missingRequired": ["liste des champs requis encore manquants"],
            "assistantMessage": "phrase à afficher à l'utilisateur",
            "suggestedFollowUps": ["max 3 suggestions courtes cliquables"],
            "status": "collecting" | "awaiting_confirmation" | "confirmed",
            "summary": "récap court si status = awaiting_confirmation ou confirmed, sinon null"
          }
        """;
    }

    private JsonNode parse(String raw) {
        try { return mapper.readTree(raw); }
        catch (Exception e) { throw new RuntimeException("Groq JSON invalide: " + raw, e); }
    }
    private Map<String, Object> toMap(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return new HashMap<>();
        return mapper.convertValue(n, Map.class);
    }
    private List<String> toStringList(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n == null || !n.isArray()) return out;
        n.forEach(e -> out.add(e.asText()));
        return out;
    }
    private String safeJson(Object o) {
        try { return mapper.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
}