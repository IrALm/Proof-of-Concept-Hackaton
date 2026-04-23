// service/AiHotelBookingService.java
package com.example.demo.service;

import com.example.demo.dto.AiBookingRequestDto;
import com.example.demo.dto.AiBookingResponseDto;
import com.example.demo.dto.AiChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class AiHotelBookingService {

    private final GroqChatService groq;
    private final SupabaseDbService db;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiHotelBookingService(GroqChatService groq, SupabaseDbService db) {
        this.groq = groq;
        this.db = db;
    }

    public AiBookingResponseDto handle(AiBookingRequestDto req) {
        Map<String, Object> ctx = req.context() == null ? Map.of() : req.context();

        List<Map<String, Object>> messages = new ArrayList<>();
        if (req.conversation() != null) {
            for (AiChatMessage m : req.conversation()) {
                messages.add(Map.of("role", m.role(), "content", m.content()));
            }
        }
        messages.add(Map.of("role", "user", "content",
                "Contexte hôtel: " + safeJson(ctx) +
                        "\nÉtat actuel: " + safeJson(req.collected() == null ? Map.of() : req.collected()) +
                        "\nNouveau message: " + req.message()));

        String raw = groq.completeJsonWithHistory(buildSystemPrompt(), messages);
        JsonNode json = parse(raw);

        Map<String, Object> collected = toMap(json.path("collected"));
        List<String> missing = toStringList(json.path("missingRequired"));
        String status = json.path("status").asText("collecting");
        String bookingId = null;

        if ("confirmed".equals(status) && missing.isEmpty()) {
            Map<String, Object> payload = buildHotelPayload(collected, ctx);
            db.insertHotelBooking(payload);
            bookingId = "ok";
        } else if ("confirmed".equals(status)) {
            status = "awaiting_confirmation";
        }

        return new AiBookingResponseDto(
                json.path("assistantMessage").asText(""),
                collected,
                missing,
                toStringList(json.path("suggestedFollowUps")),
                status,
                bookingId,
                json.path("summary").asText(null)
        );
    }

    private Map<String, Object> buildHotelPayload(Map<String, Object> collected, Map<String, Object> ctx) {
        Map<String, Object> p = new HashMap<>();
        p.put("hotel",              ctx.get("hotelName"));
        p.put("reserved_room_type", ctx.get("roomType"));
        p.put("adr",                ctx.get("adr"));

        LocalDate arrival = LocalDate.parse((String) collected.get("arrival_date"));
        p.put("arrival_date_year",        arrival.getYear());
        p.put("arrival_date_month",       arrival.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        p.put("arrival_date_week_number", arrival.get(WeekFields.ISO.weekOfWeekBasedYear()));
        p.put("arrival_date_day_of_month",arrival.getDayOfMonth());

        int nights = ((Number) collected.getOrDefault("nights", 1)).intValue();
        int weekend = countWeekendNights(arrival, nights);
        p.put("stays_in_weekend_nights", weekend);
        p.put("stays_in_week_nights",    nights - weekend);

        p.put("adults",   collected.getOrDefault("adults", 1));
        p.put("children", collected.getOrDefault("children", 0));
        p.put("babies",   collected.getOrDefault("babies", 0));
        p.put("country",  collected.get("country"));
        p.put("meal",     collected.getOrDefault("meal", "BB"));

        p.put("reservation_status",     "Booked");
        p.put("reservation_status_date", LocalDate.now().toString());
        p.put("is_canceled", false);
        p.put("lead_time", (int) ChronoUnit.DAYS.between(LocalDate.now(), arrival));

        p.values().removeIf(Objects::isNull);
        return p;
    }

    private int countWeekendNights(LocalDate start, int nights) {
        int w = 0;
        for (int i = 0; i < nights; i++) {
            var d = start.plusDays(i).getDayOfWeek();
            if (d == java.time.DayOfWeek.SATURDAY || d == java.time.DayOfWeek.SUNDAY) w++;
        }
        return w;
    }

    private String buildSystemPrompt() {
        return """
          Tu es un assistant de réservation d'hôtel. Ton rôle : collecter le MINIMUM d'infos
          en français, ton chaleureux et concis. L'hôtel, le type de chambre et le prix sont DÉJÀ
          sélectionnés (dans le contexte), tu ne les demandes pas.

          Champs REQUIS à collecter auprès de l'utilisateur (snake_case) :
          - arrival_date (YYYY-MM-DD)
          - nights (entier ≥ 1, nombre total de nuits)
          - adults (entier ≥ 1)
          - customer_first_name, customer_last_name
          - customer_email, customer_phone

          Champs OPTIONNELS : children, babies, country, meal (BB/HB/FB/SC).

          Règles :
          - Aujourd'hui : """ + LocalDate.now() + """
           . Convertis les dates relatives en absolues.
          - UN SEUL champ manquant à la fois, ordre : arrival_date → nights → adults → prénom → nom → email → téléphone.
          - Quand tout est rempli, passe en "awaiting_confirmation" avec récap dans "summary".
          - Si l'utilisateur confirme (oui/confirme/valide/ok), passe en "confirmed".

          Réponds UNIQUEMENT en JSON :
          {
            "collected": { ... },
            "missingRequired": [...],
            "assistantMessage": "...",
            "suggestedFollowUps": [...],
            "status": "collecting" | "awaiting_confirmation" | "confirmed",
            "summary": "..." | null
          }
        """;
    }

    private JsonNode parse(String raw) {
        try { return mapper.readTree(raw); } catch (Exception e) { throw new RuntimeException(e); }
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