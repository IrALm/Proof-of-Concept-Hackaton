package com.example.demo.service;

import com.example.demo.dto.AiTravelPlanIntentDto;
import com.example.demo.dto.AiTravelPlanRequestDto;
import com.example.demo.dto.AiTravelPlanResponseDto;
import com.example.demo.dto.HotelSearchResultDto;
import com.example.demo.dto.RestoSearchResultDto;
import com.example.demo.dto.UserProfileDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiTravelPlannerService {

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM PROMPTS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String INTENT_SYSTEM_PROMPT = """
            Tu es un orchestrateur de planification de voyage pour une app moderne.
            Ta mission est d'analyser une demande en langage naturel et de produire une intention voyage structuree.

            Personas:
            - LE_PLANIFICATEUR_METHODIQUE: besoin de precision, fiabilite, details concrets.
            - L_HUMANISTE: besoin de chaleur, personnalisation, ton amical.
            - LE_SPONTANE: besoin de vitesse, simplicite, peu de friction.
            - LE_COMPETITEUR: besoin de pertinence, efficacite, preuve de valeur face aux apps concurrentes.

            Regles:
            - Retourne UNIQUEMENT un objet JSON valide. Aucun texte avant ou apres. Aucun markdown.
            - Format attendu:
              {
                "persona": "...",
                "intentSummary": "...",
                "responseTone": "...",
                "destinationCountry": null,
                "destinationCity": null,
                "startDateHint": null,
                "endDateHint": null,
                "tripType": null,
                "budgetLevel": null,
                "partySize": null,
                "wantsHotel": true,
                "wantsRestaurants": true,
                "needsClarification": false,
                "missingFields": [],
                "clarifyingQuestion": null,
                "hotelLimit": 1,
                "restoLimit": 5
              }
            - `tripType` doit etre null, leisure, business, romantic, family ou adventure.
            - `budgetLevel` doit etre null, low, medium, high ou luxury.
            - `needsClarification` doit etre true si la destination (pays ou ville) est absente ou vraiment trop vague.
            - Si `needsClarification` est true, `missingFields` doit lister les champs manquants et `clarifyingQuestion` doit contenir une question courte et directe a poser a l'utilisateur.
            - Si `needsClarification` est false, les deux champs doivent etre null / vide.
            - `hotelLimit` doit etre 1 (on recommande le meilleur hotel).
            - `restoLimit` doit etre entre 3 et 5.
            - `intentSummary` doit etre une phrase courte resumant le besoin concret.
            - `responseTone` doit etre jeune, chaleureux, simple, naturel, jamais corporate.
            """;

    private static final String NARRATIVE_SYSTEM_PROMPT = """
            Tu es le travel planner d'une app voyage moderne, chaleureuse et ultra fluide.
            Tu t'adresses a des utilisateurs de 18 a 30 ans avec un ton amical, chill, credible.

            Regles:
            - Base-toi UNIQUEMENT sur les resultats fournis (hotel + restaurants).
            - Sois clair, bref et utile. Ne sur-vends pas.
            - Si peu de resultats sont disponibles, sois honnete et propose un pivot utile.
            - `greeting` doit etre une phrase courte et personnalisee (max 15 mots).
            - `summary` doit etre une description en 1 phrase du sejour propose.
            - `assistantMessage` doit etre naturel, actionnable, 2-3 phrases max.
            - `suggestedFollowUps` doit contenir exactement 3 relances courtes.
            - Retourne UNIQUEMENT un objet JSON valide. Aucun texte avant ou apres. Aucun markdown.
            """;

    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════

    private final GroqChatService groqChatService;
    private final HotelSearchService hotelSearchService;
    private final RestoSearchService restoSearchService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    public AiTravelPlannerService(GroqChatService groqChatService,
                                  HotelSearchService hotelSearchService,
                                  RestoSearchService restoSearchService,
                                  UserProfileService userProfileService,
                                  ObjectMapper objectMapper) {
        this.groqChatService = groqChatService;
        this.hotelSearchService = hotelSearchService;
        this.restoSearchService = restoSearchService;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    public AiTravelPlanResponseDto plan(String authorizationHeader, AiTravelPlanRequestDto request) {

        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        // ── Step 1: parse intent ─────────────────────────────────────────────
        AiTravelPlanIntentDto rawIntent = readJsonResponse(
                groqChatService.completeJson(INTENT_SYSTEM_PROMPT, buildIntentPrompt(request, userProfile)),
                AiTravelPlanIntentDto.class
        );

        AiTravelPlanIntentDto intent = normalizeIntent(rawIntent, request);

        // ── Step 2: clarification guard ──────────────────────────────────────
        if (Boolean.TRUE.equals(intent.needsClarification())) {
            String question = intent.clarifyingQuestion() != null
                    ? intent.clarifyingQuestion()
                    : "Peux-tu me donner plus de details sur ta destination ou la periode de ton voyage ?";
            List<String> missingFields = intent.missingFields() != null
                    ? intent.missingFields()
                    : List.of("destination");
            return AiTravelPlanResponseDto.clarification(question, missingFields);
        }

        // ── Step 3: search hotel + restaurants ───────────────────────────────
        List<HotelSearchResultDto> hotels = Boolean.FALSE.equals(intent.wantsHotel())
                ? List.of()
                : hotelSearchService.search(intent.toHotelSearchDto());

        List<RestoSearchResultDto> restaurants = Boolean.FALSE.equals(intent.wantsRestaurants())
                ? List.of()
                : restoSearchService.search(intent.toRestoSearchDto());

        HotelSearchResultDto topHotel = hotels.isEmpty() ? null : hotels.get(0);

        // ── Step 4: generate narrative ───────────────────────────────────────
        AiTravelPlanNarrativeDto narrative = readJsonResponse(
                groqChatService.completeJson(
                        NARRATIVE_SYSTEM_PROMPT,
                        buildNarrativePrompt(intent, topHotel, restaurants, userProfile)
                ),
                AiTravelPlanNarrativeDto.class
        );

        // ── Step 5: assemble final response ──────────────────────────────────
        return AiTravelPlanResponseDto.plan(
                narrative.greeting(),
                intent.persona(),
                intent.intentSummary(),
                narrative.summary(),
                narrative.assistantMessage(),
                sanitizeSuggestions(narrative.suggestedFollowUps()),
                topHotel,
                restaurants
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INTENT NORMALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    private AiTravelPlanIntentDto normalizeIntent(AiTravelPlanIntentDto rawIntent,
                                                  AiTravelPlanRequestDto request) {

        AiTravelPlanIntentDto safe = rawIntent != null
                ? rawIntent
                : AiTravelPlanIntentDto.builder().build();

        // Sensible defaults
        boolean wantsHotel       = safe.wantsHotel() == null || safe.wantsHotel();
        boolean wantsRestaurants = safe.wantsRestaurants() == null || safe.wantsRestaurants();
        boolean needsClarification = Boolean.TRUE.equals(safe.needsClarification());

        int hotelLimit = (safe.hotelLimit() != null && safe.hotelLimit() >= 1 && safe.hotelLimit() <= 5)
                ? safe.hotelLimit() : 1;
        int restoLimit = (safe.restoLimit() != null && safe.restoLimit() >= 1 && safe.restoLimit() <= 10)
                ? safe.restoLimit() : 5;

        return safe.toBuilder()
                .persona(defaultIfBlank(safe.persona(), "LE_SPONTANE"))
                .intentSummary(defaultIfBlank(safe.intentSummary(), request.message()))
                .responseTone(defaultIfBlank(safe.responseTone(), "amical, precis, chill"))
                .wantsHotel(wantsHotel)
                .wantsRestaurants(wantsRestaurants)
                .needsClarification(needsClarification)
                .hotelLimit(hotelLimit)
                .restoLimit(restoLimit)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROMPT BUILDERS
    // ═══════════════════════════════════════════════════════════════════════

    private String buildIntentPrompt(AiTravelPlanRequestDto request, UserProfileDto userProfile) {
        return """
                Analyse cette demande de voyage et produis l'intention structuree.
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.

                Profil utilisateur:
                - prenom/nom: %s
                - email: %s

                Contexte geographique (position actuelle de l'utilisateur):
                - latitude: %s
                - longitude: %s

                Message utilisateur:
                %s
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                nullToText(request.latitude()),
                nullToText(request.longitude()),
                request.message()
        );
    }

    private String buildNarrativePrompt(AiTravelPlanIntentDto intent,
                                        HotelSearchResultDto hotel,
                                        List<RestoSearchResultDto> restaurants,
                                        UserProfileDto userProfile) {
        return """
                Prepare une proposition de sejour conversationnelle, courte et utile.
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.

                Utilisateur:
                - nom affiche: %s

                Persona detectee: %s
                Intention: %s
                Type de sejour: %s
                Destination: %s
                Dates: %s → %s
                Ton attendu: %s

                Hotel propose (%s):
                %s

                Restaurants proposes (%s):
                %s
                """.formatted(
                displayName(userProfile),
                nullToText(intent.persona()),
                nullToText(intent.intentSummary()),
                nullToText(intent.tripType()),
                buildDestinationLabel(intent),
                nullToText(intent.startDateHint()),
                nullToText(intent.endDateHint()),
                nullToText(intent.responseTone()),
                hotel == null ? 0 : 1,
                summarizeHotel(hotel),
                restaurants.size(),
                summarizeRestaurants(restaurants)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUMMARIZERS
    // ═══════════════════════════════════════════════════════════════════════

    private String summarizeHotel(HotelSearchResultDto hotel) {
        if (hotel == null) {
            return "Aucun hotel trouve pour cette destination.";
        }
        return "- %s | %s | segment %s | note %s | avis %s | tarif moyen %s".formatted(
                hotel.name(),
                nullToText(hotel.country()),
                nullToText(hotel.marketSegment()),
                nullToText(hotel.rating()),
                nullToText(hotel.reviewCount()),
                nullToText(hotel.avgAdr())
        );
    }

    private String summarizeRestaurants(List<RestoSearchResultDto> restaurants) {
        if (restaurants.isEmpty()) {
            return "Aucun restaurant trouve pour cette destination.";
        }
        return restaurants.stream()
                .limit(5)
                .map(r -> "- %s | %s | %s | prix %s | note %s | distinction %s".formatted(
                        r.name(),
                        nullToText(r.location()),
                        nullToText(r.cuisine()),
                        nullToText(r.price()),
                        nullToText(r.rating()),
                        Boolean.TRUE.equals(r.greenStar()) ? "green star" : nullToText(r.award())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String buildDestinationLabel(AiTravelPlanIntentDto intent) {
        if (intent.destinationCity() != null && intent.destinationCountry() != null) {
            return intent.destinationCity() + ", " + intent.destinationCountry();
        }
        if (intent.destinationCity() != null) return intent.destinationCity();
        if (intent.destinationCountry() != null) return intent.destinationCountry();
        return "non precisee";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // JSON HELPERS  (same pattern as existing assistant services)
    // ═══════════════════════════════════════════════════════════════════════

    private <T> T readJsonResponse(String rawContent, Class<T> targetType) {
        try {
            return objectMapper.readValue(extractJson(rawContent), targetType);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI response could not be parsed as JSON",
                    e
            );
        }
    }

    private String extractJson(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "{}";
        }

        String trimmed = rawContent.trim();

        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                trimmed = trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }

        return trimmed;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY HELPERS  (mirrors the pattern in the existing services)
    // ═══════════════════════════════════════════════════════════════════════

    private List<String> sanitizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of(
                    "Trouve-moi un hotel encore mieux note",
                    "Montre-moi plus de restos dans cette ville",
                    "Propose-moi une destination alternative"
            );
        }
        return suggestions.stream()
                .filter(v -> v != null && !v.isBlank())
                .limit(3)
                .toList();
    }

    private String displayName(UserProfileDto userProfile) {
        if (userProfile == null) return "toi";
        if (userProfile.fullName() != null && !userProfile.fullName().isBlank()) {
            return userProfile.fullName();
        }
        if (userProfile.email() != null && userProfile.email().contains("@")) {
            return userProfile.email().substring(0, userProfile.email().indexOf('@'));
        }
        return "toi";
    }

    private String currentMomentOfDay() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.NOON))             return "matin";
        if (now.isBefore(LocalTime.of(18, 0)))        return "apres-midi";
        return "soir";
    }

    private String nullToText(Object value) {
        return value == null ? "non fourni" : value.toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}