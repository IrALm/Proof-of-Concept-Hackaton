package com.example.demo.service;

import com.example.demo.dto.AiHotelSearchIntentDto;
import com.example.demo.dto.AiHotelSearchRequestDto;
import com.example.demo.dto.AiHotelSearchResponseDto;
import com.example.demo.dto.AiHotelWelcomeResponseDto;
import com.example.demo.dto.HotelSearchResultDto;
import com.example.demo.dto.UserProfileDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiHotelAssistantService {

    private static final String INTENT_SYSTEM_PROMPT = """
            Tu es un moteur d'orchestration pour une app hotel moderne.
            Tu transformes une demande en langage naturel en filtres de recherche hotel fiables.

            Personas:
            - LE_PLANIFICATEUR_METHODIQUE: veut de la fiabilite et de la clarte.
            - L_HUMANISTE: veut de la chaleur et une recommandation humaine.
            - LE_SPONTANE: veut aller vite avec peu d'effort.
            - LE_COMPETITEUR: compare la pertinence et la fluidite avec d'autres apps.

            Regles:
            - Retourne uniquement un objet JSON.
            - Format attendu a la racine:
              {
                "persona": "...",
                "intentSummary": "...",
                "responseTone": "...",
                "name": null,
                "country": null,
                "marketSegment": null,
                "minRating": null,
                "maxAdr": null,
                "customerType": null,
                "isRepeatedGuest": null,
                "familyFriendly": null,
                "businessFriendly": null,
                "lowCancellation": null,
                "newOnly": null,
                "limit": 5,
                "sortBy": null,
                "sortDirection": null
              }
            - N'utilise pas de cle `filters`.
            - Si un filtre n'est pas connu, laisse-le a null.
            - `sortBy` doit etre null, score, rating, review_count, new, family, business, adr ou cancellation.
            - `sortDirection` doit etre null, asc ou desc.
            - `limit` doit etre entre 3 et 10.
            - `intentSummary` doit etre une phrase courte.
            - `responseTone` doit etre jeune, chaleureux, simple et naturel.
            """;

    private static final String WELCOME_SYSTEM_PROMPT = """
            Tu generes un message d'accueil pour une app hotel nouvelle generation.
            Le ton doit etre familial, amical, style, detendu, adapte a une audience 18-30 ans.
            Retourne uniquement un objet JSON.
            `message` doit etre court et chaleureux.
            `mood` doit etre un seul mot ou une courte expression.
            `suggestedPrompts` doit contenir 3 propositions de recherche hotel naturelles.
            """;

    private static final String NARRATIVE_SYSTEM_PROMPT = """
            Tu es le concierge hotel d'une app moderne, chaleureuse et ultra fluide.
            Tu t'adresses a des utilisateurs de 18 a 30 ans avec un ton amical, chill, credible.

            Regles:
            - Base-toi uniquement sur les hotels fournis.
            - Sois clair, bref et utile.
            - Si peu de resultats sont disponibles, sois honnete et propose un pivot utile.
            - `greeting` doit etre court et personnalise.
            - `assistantMessage` doit etre naturel et actionnable.
            - `suggestedFollowUps` doit contenir 3 idees courtes.
            - Retourne uniquement un objet JSON.
            """;

    private final GroqChatService groqChatService;
    private final HotelSearchService hotelSearchService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    public AiHotelAssistantService(GroqChatService groqChatService,
                                   HotelSearchService hotelSearchService,
                                   UserProfileService userProfileService,
                                   ObjectMapper objectMapper) {
        this.groqChatService = groqChatService;
        this.hotelSearchService = hotelSearchService;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
    }

    public AiHotelSearchResponseDto search(String authorizationHeader, AiHotelSearchRequestDto request) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        AiHotelSearchIntentDto rawIntent = readJsonResponse(
                groqChatService.completeJson(INTENT_SYSTEM_PROMPT, buildIntentPrompt(request, userProfile)),
                AiHotelSearchIntentDto.class
        );

        AiHotelSearchIntentDto normalizedIntent = normalizeIntent(rawIntent, request);
        List<HotelSearchResultDto> hotels = hotelSearchService.search(normalizedIntent.toSearchDto());

        AiHotelSearchNarrativeDto narrative = readJsonResponse(
                groqChatService.completeJson(
                        NARRATIVE_SYSTEM_PROMPT,
                        buildNarrativePrompt(normalizedIntent, hotels, userProfile)
                ),
                AiHotelSearchNarrativeDto.class
        );

        return new AiHotelSearchResponseDto(
                narrative.greeting(),
                normalizedIntent.persona(),
                normalizedIntent.intentSummary(),
                normalizedIntent,
                narrative.assistantMessage(),
                sanitizeSuggestions(narrative.suggestedFollowUps()),
                hotels
        );
    }

    public AiHotelWelcomeResponseDto welcome(String authorizationHeader) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        AiHotelWelcomeMessageDto welcomeMessage = readJsonResponse(
                groqChatService.completeJson(WELCOME_SYSTEM_PROMPT, buildWelcomePrompt(userProfile)),
                AiHotelWelcomeMessageDto.class
        );

        return new AiHotelWelcomeResponseDto(
                welcomeMessage.message(),
                welcomeMessage.mood(),
                sanitizeSuggestions(welcomeMessage.suggestedPrompts())
        );
    }

    private AiHotelSearchIntentDto normalizeIntent(AiHotelSearchIntentDto rawIntent, AiHotelSearchRequestDto request) {
        AiHotelSearchIntentDto safeIntent = rawIntent == null ? AiHotelSearchIntentDto.builder().build() : rawIntent;
        Integer limit = safeIntent.limit() != null ? safeIntent.limit() : request.limit();

        if (limit == null || limit < 3 || limit > 10) {
            limit = 5;
        }

        return safeIntent.toBuilder()
                .persona(defaultIfBlank(safeIntent.persona(), "LE_SPONTANE"))
                .intentSummary(defaultIfBlank(safeIntent.intentSummary(), request.message()))
                .responseTone(defaultIfBlank(safeIntent.responseTone(), "amical, precis, chill"))
                .limit(limit)
                .sortBy(normalizeSortBy(safeIntent.sortBy()))
                .sortDirection(normalizeSortDirection(safeIntent.sortDirection()))
                .build();
    }

    private <T> T readJsonResponse(String rawContent, Class<T> targetType) {
        try {
            String normalizedJson = normalizeJsonForTarget(extractJson(rawContent), targetType);
            return objectMapper.readValue(normalizedJson, targetType);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response could not be parsed as JSON", e);
        }
    }

    private <T> String normalizeJsonForTarget(String rawJson, Class<T> targetType) throws JsonProcessingException {
        if (!AiHotelSearchIntentDto.class.equals(targetType)) {
            return rawJson;
        }

        JsonNode root = objectMapper.readTree(rawJson);
        if (!root.isObject()) {
            return rawJson;
        }

        JsonNode filtersNode = root.get("filters");
        if (filtersNode == null || !filtersNode.isObject()) {
            return rawJson;
        }

        ObjectNode merged = objectMapper.createObjectNode();
        merged.setAll((ObjectNode) filtersNode);

        copyIfPresent(root, merged, "persona");
        copyIfPresent(root, merged, "intentSummary");
        copyIfPresent(root, merged, "responseTone");
        copyIfPresent(root, merged, "limit");
        copyIfPresent(root, merged, "sortBy");
        copyIfPresent(root, merged, "sortDirection");
        copyIfPresent(root, merged, "newOnly");

        return objectMapper.writeValueAsString(merged);
    }

    private void copyIfPresent(JsonNode source, ObjectNode target, String fieldName) {
        JsonNode value = source.get(fieldName);
        if (value != null && !value.isMissingNode()) {
            target.set(fieldName, value);
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

    private String buildIntentPrompt(AiHotelSearchRequestDto request, UserProfileDto userProfile) {
        return """
                Analyse cette demande et extrais les filtres de recherche hotel les plus utiles.
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.

                Profil utilisateur:
                - prenom/nom: %s
                - email: %s

                Message utilisateur:
                %s
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                request.message()
        );
    }

    private String buildNarrativePrompt(AiHotelSearchIntentDto intent,
                                        List<HotelSearchResultDto> hotels,
                                        UserProfileDto userProfile) {
        return """
                Prepare une reponse conversationnelle courte et utile.
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.

                Utilisateur:
                - nom affiche: %s

                Persona detectee: %s
                Intention: %s
                Ton attendu: %s

                Resultats (%s):
                %s
                """.formatted(
                displayName(userProfile),
                nullToText(intent.persona()),
                nullToText(intent.intentSummary()),
                nullToText(intent.responseTone()),
                hotels.size(),
                summarizeHotels(hotels)
        );
    }

    private String buildWelcomePrompt(UserProfileDto userProfile) {
        return """
                Genere un accueil personnalise pour l'utilisateur suivant:
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.
                - nom affiche: %s
                - email: %s
                - moment de la journee: %s

                L'app aide a trouver des hotels avec une recommandation intelligente et fluide.
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                currentMomentOfDay()
        );
    }

    private String summarizeHotels(List<HotelSearchResultDto> hotels) {
        if (hotels.isEmpty()) {
            return "Aucun hotel trouve pour cette demande.";
        }

        return hotels.stream()
                .limit(5)
                .map(hotel -> """
                        - %s | %s | segment %s | note %s | avis %s | adr moyen %s | annulation %s
                        """.formatted(
                        hotel.name(),
                        nullToText(hotel.country()),
                        nullToText(hotel.marketSegment()),
                        nullToText(hotel.rating()),
                        nullToText(hotel.reviewCount()),
                        nullToText(hotel.avgAdr()),
                        nullToText(hotel.cancellationRate())
                ))
                .collect(Collectors.joining());
    }

    private List<String> sanitizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of(
                    "Trouve-moi un hotel fiable pour ce week-end",
                    "Je veux un hotel bien note pas trop cher",
                    "Montre-moi des hotels adaptes a un voyage business"
            );
        }

        return suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(3)
                .toList();
    }

    private String displayName(UserProfileDto userProfile) {
        if (userProfile == null) {
            return "toi";
        }

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

        if (now.isBefore(LocalTime.NOON)) {
            return "matin";
        }
        if (now.isBefore(LocalTime.of(18, 0))) {
            return "apres-midi";
        }
        return "soir";
    }

    private String nullToText(Object value) {
        return value == null ? "non fourni" : value.toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeSortBy(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "score", "rating", "review_count", "new", "family", "business", "adr", "cancellation" -> normalized;
            default -> null;
        };
    }

    private String normalizeSortDirection(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "asc", "desc" -> normalized;
            default -> null;
        };
    }
}
