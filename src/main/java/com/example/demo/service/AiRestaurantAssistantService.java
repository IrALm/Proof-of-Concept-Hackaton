package com.example.demo.service;

import com.example.demo.dto.AiSearchIntentDto;
import com.example.demo.dto.AiSearchRequestDto;
import com.example.demo.dto.AiSearchResponseDto;
import com.example.demo.dto.AiWelcomeResponseDto;
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
public class AiRestaurantAssistantService {

    private static final String INTENT_SYSTEM_PROMPT = """
            Tu es un moteur d'orchestration pour une app Michelin moderne.
            Ta mission est d'extraire l'essentiel d'une demande utilisateur en langage naturel
            et de la convertir en filtres de recherche fiables.

            Tu dois raisonner pour 4 personas:
            - LE_PLANIFICATEUR_METHODIQUE: besoin de precision, fiabilite, details concrets.
            - L_HUMANISTE: besoin de chaleur, personnalisation, ton amical.
            - LE_SPONTANE: besoin de vitesse, simplicite, peu de friction.
            - LE_COMPETITEUR: besoin de pertinence, efficacite, preuve de valeur face aux apps concurrentes.

            Regles:
            - Retourne uniquement un objet JSON.
            - Si l'utilisateur n'a pas donne un filtre, laisse-le a null.
            - `price` doit etre null ou l'une des formes €, €€, €€€, €€€€.
            - `sortBy` doit etre null, score, new ou recent.
            - `sortDirection` doit etre null, asc ou desc.
            - `limit` doit rester entre 3 et 10. Favorise 5 pour une recherche conversationnelle.
            - Si l'utilisateur veut quelque chose pres de moi, reutilise latitude/longitude si disponibles.
            - Si la demande evoque nouveau, nouveautes, recent, active `newOnly=true`.
            - `intentSummary` doit etre une phrase courte resumant le besoin concret.
            - `responseTone` doit etre bref, chaleureux, jeune, naturel, jamais corporate.
            """;

    private static final String WELCOME_SYSTEM_PROMPT = """
            Tu generes un message d'accueil pour une app Michelin nouvelle generation.
            Le ton doit etre familial, amical, style, detendu, adapte a une audience 18-30 ans.
            Retourne uniquement un objet JSON.
            `message` doit etre court et chaleureux.
            `mood` doit etre un seul mot ou une courte expression.
            `suggestedPrompts` doit contenir 3 propositions de recherche naturelles.
            """;

    private static final String NARRATIVE_SYSTEM_PROMPT = """
            Tu es le concierge Michelin d'une app food moderne, chaleureuse et ultra fluide.
            Tu t'adresses a des utilisateurs de 18 a 30 ans avec un ton amical, chill, credible.
            Tu ne sur-vends pas. Tu vas droit au but, tout en gardant une vibe humaine.

            Regles:
            - Base-toi uniquement sur les restaurants fournis.
            - Mets en avant la pertinence, la qualite Michelin et la simplicite.
            - Si peu de resultats sont disponibles, sois honnete et propose un pivot utile.
            - `greeting` doit etre court et personnalise.
            - `assistantMessage` doit etre clair, naturel et actionnable.
            - `suggestedFollowUps` doit contenir 3 idees de relance courtes.
            - Retourne uniquement un objet JSON.
            """;

    private final GroqChatService groqChatService;
    private final RestoSearchService restoSearchService;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    public AiRestaurantAssistantService(GroqChatService groqChatService,
                                        RestoSearchService restoSearchService,
                                        UserProfileService userProfileService,
                                        ObjectMapper objectMapper) {
        this.groqChatService = groqChatService;
        this.restoSearchService = restoSearchService;
        this.userProfileService = userProfileService;
        this.objectMapper = objectMapper;
    }

    public AiSearchResponseDto search(String authorizationHeader, AiSearchRequestDto request) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        AiSearchIntentDto rawIntent = readJsonResponse(
                groqChatService.completeJson(INTENT_SYSTEM_PROMPT, buildIntentPrompt(request, userProfile)),
                AiSearchIntentDto.class
        );

        AiSearchIntentDto normalizedIntent = normalizeIntent(rawIntent, request);
        List<RestoSearchResultDto> restaurants = restoSearchService.search(normalizedIntent.toSearchDto());

        AiSearchNarrativeDto narrative = readJsonResponse(
                groqChatService.completeJson(
                        NARRATIVE_SYSTEM_PROMPT,
                        buildNarrativePrompt(normalizedIntent, restaurants, userProfile)
                ),
                AiSearchNarrativeDto.class
        );

        return new AiSearchResponseDto(
                narrative.greeting(),
                normalizedIntent.persona(),
                normalizedIntent.intentSummary(),
                normalizedIntent,
                narrative.assistantMessage(),
                sanitizeSuggestions(narrative.suggestedFollowUps()),
                restaurants
        );
    }

    public AiWelcomeResponseDto welcome(String authorizationHeader) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        AiWelcomeMessageDto welcomeMessage = readJsonResponse(
                groqChatService.completeJson(WELCOME_SYSTEM_PROMPT, buildWelcomePrompt(userProfile)),
                AiWelcomeMessageDto.class
        );

        return new AiWelcomeResponseDto(
                welcomeMessage.message(),
                welcomeMessage.mood(),
                sanitizeSuggestions(welcomeMessage.suggestedPrompts())
        );
    }

    private AiSearchIntentDto normalizeIntent(AiSearchIntentDto rawIntent, AiSearchRequestDto request) {
        AiSearchIntentDto safeIntent = rawIntent == null ? AiSearchIntentDto.builder().build() : rawIntent;

        Double latitude = safeIntent.latitude() != null ? safeIntent.latitude() : request.latitude();
        Double longitude = safeIntent.longitude() != null ? safeIntent.longitude() : request.longitude();
        Double radius = safeIntent.radius() != null ? safeIntent.radius() : request.radius();
        Integer limit = safeIntent.limit() != null ? safeIntent.limit() : request.limit();

        if (limit == null || limit < 3 || limit > 10) {
            limit = 5;
        }

        if (latitude != null && longitude != null && radius == null) {
            radius = 5000d;
        }

        return safeIntent.toBuilder()
                .persona(defaultIfBlank(safeIntent.persona(), "LE_SPONTANE"))
                .intentSummary(defaultIfBlank(safeIntent.intentSummary(), request.message()))
                .responseTone(defaultIfBlank(safeIntent.responseTone(), "amical, precis, chill"))
                .latitude(latitude)
                .longitude(longitude)
                .radius(radius)
                .limit(limit)
                .sortBy(normalizeSortBy(safeIntent.sortBy()))
                .sortDirection(normalizeSortDirection(safeIntent.sortDirection()))
                .build();
    }

    private <T> T readJsonResponse(String rawContent, Class<T> targetType) {
        try {
            return objectMapper.readValue(extractJson(rawContent), targetType);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response could not be parsed as JSON", e);
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

    private String buildIntentPrompt(AiSearchRequestDto request, UserProfileDto userProfile) {
        return """
                Analyse cette demande et extrais les filtres de recherche restaurant les plus utiles.
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.

                Profil utilisateur:
                - prenom/nom: %s
                - email: %s

                Contexte geographique fourni:
                - latitude: %s
                - longitude: %s
                - radius: %s

                Message utilisateur:
                %s
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                nullToText(request.latitude()),
                nullToText(request.longitude()),
                nullToText(request.radius()),
                request.message()
        );
    }

    private String buildNarrativePrompt(AiSearchIntentDto intent,
                                        List<RestoSearchResultDto> restaurants,
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
                restaurants.size(),
                summarizeRestaurants(restaurants)
        );
    }

    private String buildWelcomePrompt(UserProfileDto userProfile) {
        return """
                Genere un accueil personnalise pour l'utilisateur suivant:
                Reponds uniquement avec du JSON valide. Aucun texte avant ou apres. Aucun markdown.
                - nom affiche: %s
                - email: %s
                - moment de la journee: %s

                L'app aide a trouver des restaurants du Guide Michelin de facon simple et moderne.
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                currentMomentOfDay()
        );
    }

    private String summarizeRestaurants(List<RestoSearchResultDto> restaurants) {
        if (restaurants.isEmpty()) {
            return "Aucun restaurant trouve pour cette demande.";
        }

        return restaurants.stream()
                .limit(5)
                .map(restaurant -> """
                        - %s | %s | %s | prix %s | note %s | avis %s | distinction %s
                        """.formatted(
                        restaurant.name(),
                        nullToText(restaurant.location()),
                        nullToText(restaurant.cuisine()),
                        nullToText(restaurant.price()),
                        nullToText(restaurant.rating()),
                        nullToText(restaurant.reviewCount()),
                        restaurant.greenStar() != null && restaurant.greenStar()
                                ? "green star"
                                : nullToText(restaurant.award())
                ))
                .collect(Collectors.joining());
    }

    private List<String> sanitizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of(
                    "Trouve-moi quelque chose de cool pres de moi",
                    "Montre-moi les meilleures tables Michelin a Paris",
                    "Je veux une adresse spontanee pour ce soir"
            );
        }

        return suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(3)
                .toList();
    }

    private String displayName(UserProfileDto userProfile) {
        if (userProfile == null) {
            return "there";
        }

        if (userProfile.fullName() != null && !userProfile.fullName().isBlank()) {
            return userProfile.fullName();
        }

        if (userProfile.email() != null && userProfile.email().contains("@")) {
            return userProfile.email().substring(0, userProfile.email().indexOf('@'));
        }

        return "there";
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
            case "new", "recent", "score" -> normalized;
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
