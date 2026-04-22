package com.example.demo.service;

import com.example.demo.dto.AiTravelPlanConversationMessageDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Travel planner service built on the EXACT same conversational pattern
 * as {@link AiProfileService}:
 *
 * <ol>
 *   <li>One LLM call per user turn, with the full conversation history.</li>
 *   <li>The LLM returns a JSON object with {@code isReadyToPlan} (equivalent to
 *       {@code isComplete} in the profile flow).</li>
 *   <li>As long as {@code isReadyToPlan = false}, we return a chat turn (a
 *       question + quick-reply chips) to the user and wait for the next turn.</li>
 *   <li>The FIRST time {@code isReadyToPlan = true}, we trigger the hotel +
 *       restaurant search and a second LLM call for the narrative. Everything
 *       is returned in the SAME response.</li>
 * </ol>
 *
 * <p>Important: if the user's very first message is already detailed enough,
 * the LLM returns {@code isReadyToPlan = true} immediately and the user gets
 * a plan in a single round-trip.
 */
@Service
public class AiTravelPlannerService {

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM PROMPTS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String CHAT_SYSTEM_PROMPT = """
            Tu es le travel planner d'une app voyage moderne, chaleureuse et ultra fluide.
            Tu t'adresses a des utilisateurs de 18 a 30 ans avec un ton amical, chill et direct.

            Ta mission: collecter le strict minimum d'informations pour proposer un sejour
            (hotel + restos), puis declencher la proposition.

            Champs a couvrir (par ordre de priorite):
            1. destination (pays ou ville) — OBLIGATOIRE
            2. type de voyage: leisure, business, romantic, family, adventure
            3. budget: low, medium, high, luxury
            4. periode (dates ou hint temporel)
            5. taille du groupe

            Regles de conversation:
            - Pose UNE seule question par tour, courte et directe.
            - Si l'utilisateur donne une reponse vague, accepte-la et passe au suivant.
            - Si la destination est donnee ET au moins un autre champ est fourni (ou deductible
              du contexte, ex: "week-end romantique" → tripType=romantic), passe `isReadyToPlan` a true.
            - Si le tout premier message est deja complet (destination + type + periode par ex.),
              mets `isReadyToPlan` a true directement sans poser de question.
            - Ne repose JAMAIS deux fois la meme question dans la conversation. Si tu l'as deja posee
              et que la reponse est floue, considere-la comme satisfaite et passe a la suivante.
              Apres 2 tours assistant deja echanges, force `isReadyToPlan = true` avec les infos dispo.
            - Si l'utilisateur confirme une proposition que tu viens de faire, mets `isReadyToPlan` a true.

            Remplissage de collectedIntent:
            - Remplis ce que tu as deduit au fil des tours (laisse les autres champs a null).
            - tripType: leisure | business | romantic | family | adventure (ou null)
            - budgetLevel: low | medium | high | luxury (ou null)
            - wantsHotel et wantsRestaurants: true par defaut, sauf si l'utilisateur exclut explicitement.
            - hotelLimit: toujours 1. restoLimit: entre 3 et 5.
            - persona: LE_PLANIFICATEUR_METHODIQUE | L_HUMANISTE | LE_SPONTANE | LE_COMPETITEUR
            - responseTone: court descriptif (ex: "amical, direct, chill")
            - intentSummary: phrase courte resumant le besoin.

            Format de sortie: UNIQUEMENT un objet JSON valide. Aucun texte avant ou apres. Aucun markdown.
            {
              "responseMessage": "...",
              "isReadyToPlan": false,
              "completionScore": 0,
              "collectedIntent": {
                "persona": null,
                "intentSummary": null,
                "responseTone": null,
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
              },
              "suggestedReplies": ["...", "...", "..."]
            }
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

    /**
     * Opens a new travel planning conversation.
     *
     * <p>Returns a hardcoded welcome message with starter chips — <strong>no
     * Groq call is made</strong>. This mirrors {@code AiProfileService.start()}
     * conceptually but saves tokens on the most common action (opening the
     * screen). Personalization is done server-side with the user's display name.
     */
    public AiTravelPlanResponseDto start(String authorizationHeader) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);
        String firstName = displayName(userProfile);

        String welcome = """
            Salut %s ! 👋 Prêt·e pour un petit voyage ? \
            Dis-moi où tu penses aller (ou l'ambiance que tu cherches) et je m'occupe du reste.\
            """.formatted(firstName);

        List<String> starterChips = List.of(
                "Rome 🇮🇹",
                "Week-end romantique",
                "Escapade nature"
        );

        return AiTravelPlanResponseDto.chatTurn(welcome, starterChips);
    }


    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Single chat turn. Same behavior as {@code AiProfileService.chat}:
     * <ul>
     *   <li>As long as the LLM decides it's not ready, we return a question.</li>
     *   <li>The FIRST time the LLM flips {@code isReadyToPlan} to true, we run
     *       the hotel + restaurant search and the narrative step here, and
     *       return the complete plan in the same response.</li>
     * </ul>
     */
    public AiTravelPlanResponseDto plan(String authorizationHeader, AiTravelPlanRequestDto request) {

        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        // ── Step 1: single LLM chat call with the full conversation history ──
        AiTravelPlanChatGroqDto groqResponse = readJsonResponse(
                groqChatService.completeJsonWithHistory(
                        CHAT_SYSTEM_PROMPT,
                        buildHistoryMessages(request, userProfile)
                ),
                AiTravelPlanChatGroqDto.class
        );

        // ── Step 2: decide if we must trigger the plan ───────────────────────
        //
        // Two safety nets on top of the LLM's own decision:
        //  - after 2+ assistant turns, force isReadyToPlan = true to prevent
        //    the LLM from asking questions forever;
        //  - if somehow destination is still missing, we fall back to a chat
        //    turn BUT only if we haven't already asked — so max 1 clarification.
        //
        boolean llmSaysReady       = Boolean.TRUE.equals(groqResponse.isReadyToPlan());
        int assistantTurnsSoFar    = countAssistantTurns(request.history());
        boolean forceReadyByTurns  = assistantTurnsSoFar >= 2;
        AiTravelPlanIntentDto intent = normalizeIntent(groqResponse.collectedIntent(), request);
        boolean hasDestination     = hasDestination(intent);

        boolean shouldTriggerPlan = (llmSaysReady || forceReadyByTurns) && hasDestination;

        if (!shouldTriggerPlan) {
            // ── Still chatting: just return the question + quick replies ─────
            return AiTravelPlanResponseDto.chatTurn(
                    defaultIfBlank(groqResponse.responseMessage(), "Dis m'en un peu plus ?"),
                    sanitizeSuggestions(groqResponse.suggestedReplies(), defaultChatReplies())
            );
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
                defaultIfBlank(groqResponse.responseMessage(), "C'est parti !"),
                sanitizeSuggestions(narrative.suggestedFollowUps(), defaultPlanFollowUps()),
                narrative.greeting(),
                intent.persona(),
                intent.intentSummary(),
                narrative.summary(),
                narrative.assistantMessage(),
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

        boolean wantsHotel       = safe.wantsHotel() == null || safe.wantsHotel();
        boolean wantsRestaurants = safe.wantsRestaurants() == null || safe.wantsRestaurants();

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
                .needsClarification(false)  // now handled by isReadyToPlan
                .hotelLimit(hotelLimit)
                .restoLimit(restoLimit)
                .build();
    }

    private boolean hasDestination(AiTravelPlanIntentDto intent) {
        return (intent.destinationCity() != null && !intent.destinationCity().isBlank())
                || (intent.destinationCountry() != null && !intent.destinationCountry().isBlank());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVERSATION HISTORY BUILDER  (mirrors AiProfileService)
    // ═══════════════════════════════════════════════════════════════════════

    private List<Map<String, Object>> buildHistoryMessages(AiTravelPlanRequestDto request,
                                                           UserProfileDto userProfile) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // Fixed context as a leading system message (profile + geoloc).
        messages.add(Map.of(
                "role", "system",
                "content", buildContextBlock(request, userProfile)
        ));

        // Full conversation history.
        if (request.history() != null) {
            for (AiTravelPlanConversationMessageDto msg : request.history()) {
                if (msg.role() != null && msg.content() != null && !msg.content().isBlank()) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
        }

        // Current user turn.
        if (request.message() != null && !request.message().isBlank()) {
            messages.add(Map.of("role", "user", "content", request.message()));
        }

        return messages;
    }

    private int countAssistantTurns(List<AiTravelPlanConversationMessageDto> history) {
        if (history == null) return 0;
        return (int) history.stream()
                .filter(m -> m != null && "assistant".equalsIgnoreCase(m.role()))
                .count();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROMPT BUILDERS
    // ═══════════════════════════════════════════════════════════════════════

    private String buildContextBlock(AiTravelPlanRequestDto request, UserProfileDto userProfile) {
        return """
                Contexte fixe de la conversation (ne pas repeter a l'utilisateur):

                Profil utilisateur:
                - prenom/nom: %s
                - email: %s

                Contexte geographique (position actuelle de l'utilisateur):
                - latitude: %s
                - longitude: %s

                Moment de la journee cote utilisateur: %s

                Rappel: utilise TOUTE la conversation ci-dessous pour construire l'intention finale,
                et ne repose jamais une question deja posee.
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu",
                nullToText(request.latitude()),
                nullToText(request.longitude()),
                currentMomentOfDay()
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
    // JSON HELPERS
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
        if (rawContent == null || rawContent.isBlank()) return "{}";

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
    // UTILITY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private List<String> sanitizeSuggestions(List<String> suggestions, List<String> fallback) {
        if (suggestions == null || suggestions.isEmpty()) return fallback;
        List<String> cleaned = suggestions.stream()
                .filter(v -> v != null && !v.isBlank())
                .limit(3)
                .toList();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private List<String> defaultChatReplies() {
        return List.of("Rome 🇮🇹", "Tokyo 🗾", "Bali 🌴");
    }

    private List<String> defaultPlanFollowUps() {
        return List.of(
                "Trouve-moi un hotel encore mieux note",
                "Montre-moi plus de restos dans cette ville",
                "Propose-moi une destination alternative"
        );
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
        if (now.isBefore(LocalTime.NOON))      return "matin";
        if (now.isBefore(LocalTime.of(18, 0))) return "apres-midi";
        return "soir";
    }

    private String nullToText(Object value) {
        return value == null ? "non fourni" : value.toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}