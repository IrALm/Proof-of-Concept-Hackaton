package com.example.demo.service;

import com.example.demo.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiProfileService {

    // ═══════════════════════════════════════════════════════════════════════
    // SYSTEM PROMPTS
    // ═══════════════════════════════════════════════════════════════════════

    private static final String START_SYSTEM_PROMPT = """
            Tu démarres une session de personnalisation de profil pour une app voyage et gastronomie moderne.
            Tu t'adresses à des utilisateurs de 18 à 30 ans avec un ton amical, chill et direct.

            Ta première réponse doit :
            1. Saluer l'utilisateur par son prénom si disponible.
            2. Expliquer en 1 phrase que tu vas personnaliser son expérience.
            3. Mentionner qu'il peut coller un lien Instagram ou TikTok public pour aller plus vite.
            4. Poser immédiatement la première question : ses cuisines préférées.

            Retourne UNIQUEMENT un objet JSON valide. Aucun texte avant ou après. Aucun markdown.
            Format attendu :
            {
              "responseMessage": "...",
              "isComplete": false,
              "completionScore": 0,
              "extractedPreferences": {
                "restaurants": { "cuisines": [], "priceRange": [], "atmosphere": [], "dietaryRestrictions": [] },
                "hotels": { "amenities": [], "tripTypes": [], "budgetLevel": null, "preferredSegments": [] },
                "travel": { "favoriteDestinations": [], "travelStyle": null }
              },
              "suggestedReplies": ["Italienne 🍝", "Japonaise 🍣", "Je mange de tout !"]
            }
            """;

    private static final String CHAT_SYSTEM_PROMPT = """
            Tu es un assistant de personnalisation pour une app voyage et gastronomie.
            Ta mission : collecter les préférences de l'utilisateur en posant UNE seule question à la fois.

            Thèmes à couvrir dans cet ordre :
            1. Cuisines préférées (italienne, japonaise, française, etc.)
            2. Gamme de prix restaurants (€, €€, €€€, €€€€)
            3. Ambiance restaurants (romantique, cosy, animé, gastronomique, etc.)
            4. Restrictions alimentaires (végétarien, vegan, halal, sans gluten, aucune, etc.)
            5. Équipements hôtel souhaités (piscine, spa, gym, vue mer, petit-déjeuner inclus, etc.)
            6. Type de voyage (famille, romantique, business, aventure, détente)
            7. Budget hôtel (low, medium, high, luxury)
            8. Destinations favorites ou rêvées

            Règles :
            - Pose UNE seule question par tour.
            - Sois naturel, jeune, friendly. Jamais corporate.
            - Si l'utilisateur donne une réponse vague, accepte-la et passe au thème suivant.
            - Si le message contient une annotation [Contexte système: ...], tiens-en compte silencieusement et continue la conversation naturellement.
            - Quand tu as couvert au moins 5 thèmes, mets isComplete: true et propose un récapitulatif.
            - completionScore = pourcentage de thèmes couverts sur 8 (0 à 100).
            - Dans extractedPreferences, accumule tout ce qui a été mentionné jusqu'ici (listes vides si rien encore).
            - Si l'utilisateur confirme des préférences déjà proposées (scraping ou récap), mets isComplete: true.
            - suggestedReplies : 2 à 3 réponses rapides pertinentes au contexte.

            Retourne UNIQUEMENT un objet JSON valide. Aucun texte avant ou après. Aucun markdown.
            Format attendu :
            {
              "responseMessage": "...",
              "isComplete": false,
              "completionScore": 0,
              "extractedPreferences": {
                "restaurants": { "cuisines": [], "priceRange": [], "atmosphere": [], "dietaryRestrictions": [] },
                "hotels": { "amenities": [], "tripTypes": [], "budgetLevel": null, "preferredSegments": [] },
                "travel": { "favoriteDestinations": [], "travelStyle": null }
              },
              "suggestedReplies": []
            }
            """;

    private static final String SCRAPE_SYSTEM_PROMPT = """
            Tu as reçu du contenu public extrait d'un profil social (Instagram, TikTok, etc.).
            Analyse ce contenu et extrais les préférences culinaires, hôtelières et de voyage de l'utilisateur.

            Règles :
            - Déduis les préférences uniquement à partir du contenu fourni. N'invente rien.
            - Si tu trouves des infos pertinentes, propose-les à l'utilisateur pour confirmation.
            - Si tu ne peux pas déduire grand chose, sois honnête : dis que tu n'as pas trouvé assez d'infos et continue avec des questions.
            - isComplete doit toujours être false après une analyse de scraping (on attend la confirmation de l'utilisateur).
            - completionScore = estimation du % de thèmes couverts par les données trouvées.
            - suggestedReplies : ["Ça me correspond !", "Je veux modifier quelques choses", "Continue avec des questions"]

            Retourne UNIQUEMENT un objet JSON valide. Aucun texte avant ou après. Aucun markdown.
            Format attendu :
            {
              "responseMessage": "...",
              "isComplete": false,
              "completionScore": 0,
              "extractedPreferences": {
                "restaurants": { "cuisines": [], "priceRange": [], "atmosphere": [], "dietaryRestrictions": [] },
                "hotels": { "amenities": [], "tripTypes": [], "budgetLevel": null, "preferredSegments": [] },
                "travel": { "favoriteDestinations": [], "travelStyle": null }
              },
              "suggestedReplies": ["Ça me correspond !", "Je veux modifier quelques choses", "Continue avec des questions"]
            }
            """;

    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final GroqChatService groqChatService;
    private final SocialScraperService socialScraperService;
    private final UserProfileService userProfileService;
    private final SupabaseDbService supabaseDbService;
    private final ObjectMapper objectMapper;

    public AiProfileService(GroqChatService groqChatService,
                            SocialScraperService socialScraperService,
                            UserProfileService userProfileService,
                            SupabaseDbService supabaseDbService,
                            ObjectMapper objectMapper) {
        this.groqChatService = groqChatService;
        this.socialScraperService = socialScraperService;
        this.userProfileService = userProfileService;
        this.supabaseDbService = supabaseDbService;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts the profile completion conversation.
     * Returns the IA's opening message with the first question.
     */
    public AiProfileChatResponseDto start(String authorizationHeader) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        AiProfileChatGroqDto groqResponse = readJsonResponse(
                groqChatService.completeJson(START_SYSTEM_PROMPT, buildStartPrompt(userProfile)),
                AiProfileChatGroqDto.class
        );

        return toResponseDto(groqResponse);
    }

    /**
     * Processes one turn of the profile conversation.
     *
     * <p>Internally handles three cases transparently:
     * <ol>
     *   <li><strong>URL detected + scrape succeeds</strong> — IA analyzes the scraped content
     *       and proposes preferences for confirmation.</li>
     *   <li><strong>URL detected + scrape fails</strong> — IA is informed via a silent system
     *       annotation and continues with direct questions.</li>
     *   <li><strong>Normal message</strong> — multi-turn conversation continues.</li>
     * </ol>
     */
    public AiProfileChatResponseDto chat(String authorizationHeader, AiProfileChatRequestDto request) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        Optional<String> detectedUrl = detectUrl(request.message());

        AiProfileChatGroqDto groqResponse;

        if (detectedUrl.isPresent()) {
            Optional<String> scrapedContent = socialScraperService.extractPublicContent(detectedUrl.get());

            if (scrapedContent.isPresent()) {
                // ── Scrape succeeded → single-turn analysis ───────────────
                groqResponse = readJsonResponse(
                        groqChatService.completeJson(
                                SCRAPE_SYSTEM_PROMPT,
                                buildScrapePrompt(scrapedContent.get(), userProfile)
                        ),
                        AiProfileChatGroqDto.class
                );
            } else {
                // ── Scrape failed → continue conversation with silent note ─
                String messageWithNote = request.message()
                        + "\n\n[Contexte système: le profil social n'a pas pu être analysé. "
                        + "Informe l'utilisateur gentiment qu'on n'a rien trouvé sur ce profil "
                        + "et continue avec la prochaine question de la conversation.]";

                groqResponse = readJsonResponse(
                        groqChatService.completeJsonWithHistory(
                                CHAT_SYSTEM_PROMPT,
                                buildHistoryMessages(request.history(), messageWithNote)
                        ),
                        AiProfileChatGroqDto.class
                );
            }
        } else {
            // ── Normal conversational turn ────────────────────────────────
            groqResponse = readJsonResponse(
                    groqChatService.completeJsonWithHistory(
                            CHAT_SYSTEM_PROMPT,
                            buildHistoryMessages(request.history(), request.message())
                    ),
                    AiProfileChatGroqDto.class
            );
        }

        return toResponseDto(groqResponse);
    }

    /**
     * Persists the confirmed preferences to Supabase.
     * Requires a valid Authorization header.
     */
    public AiProfileSaveResponseDto save(String authorizationHeader, AiProfileSaveRequestDto request) {
        UserProfileDto userProfile = userProfileService.resolveUserProfile(authorizationHeader);

        if (userProfile == null || userProfile.id() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required to save preferences"
            );
        }

        supabaseDbService.updatePreferences(userProfile.id(), request.preferences());

        return new AiProfileSaveResponseDto(true, "Tes préférences ont bien été enregistrées 🎉");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PROMPT BUILDERS
    // ═══════════════════════════════════════════════════════════════════════

    private String buildStartPrompt(UserProfileDto userProfile) {
        return """
                Démarre la session de personnalisation pour cet utilisateur.
                Réponds uniquement avec du JSON valide. Aucun texte avant ou après. Aucun markdown.

                Profil utilisateur :
                - nom affiché : %s
                - email : %s
                """.formatted(
                displayName(userProfile),
                userProfile != null ? nullToText(userProfile.email()) : "inconnu"
        );
    }

    private String buildScrapePrompt(String scrapedContent, UserProfileDto userProfile) {
        return """
                Analyse ce contenu public extrait d'un profil social et extrais les préférences.
                Réponds uniquement avec du JSON valide. Aucun texte avant ou après. Aucun markdown.

                Utilisateur : %s

                Contenu extrait :
                %s
                """.formatted(
                displayName(userProfile),
                scrapedContent
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONVERSATION HISTORY BUILDER
    // ═══════════════════════════════════════════════════════════════════════

    private List<Map<String, Object>> buildHistoryMessages(
            List<AiProfileConversationMessageDto> history,
            String currentUserMessage) {

        List<Map<String, Object>> messages = new ArrayList<>();

        if (history != null) {
            for (AiProfileConversationMessageDto msg : history) {
                if (msg.role() != null && msg.content() != null) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
        }

        messages.add(Map.of("role", "user", "content", currentUserMessage));
        return messages;
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
    // MAPPING & UTILITIES
    // ═══════════════════════════════════════════════════════════════════════

    private AiProfileChatResponseDto toResponseDto(AiProfileChatGroqDto groqDto) {
        if (groqDto == null) {
            return new AiProfileChatResponseDto(
                    "Oups, une erreur est survenue. Réessaie !",
                    false,
                    emptyPreferences(),
                    List.of("Réessayer", "Continuer")
            );
        }

        boolean isComplete = Boolean.TRUE.equals(groqDto.isComplete());

        AiProfilePreferencesDto preferences = groqDto.extractedPreferences() != null
                ? groqDto.extractedPreferences()
                : emptyPreferences();

        List<String> suggestions = sanitizeSuggestions(groqDto.suggestedReplies());

        return new AiProfileChatResponseDto(
                defaultIfBlank(groqDto.responseMessage(), "Je continue..."),
                isComplete,
                preferences,
                suggestions
        );
    }

    private AiProfilePreferencesDto emptyPreferences() {
        return new AiProfilePreferencesDto(
                new AiProfilePreferencesDto.RestaurantPreferences(List.of(), List.of(), List.of(), List.of()),
                new AiProfilePreferencesDto.HotelPreferences(List.of(), List.of(), null, List.of()),
                new AiProfilePreferencesDto.TravelPreferences(List.of(), null)
        );
    }

    private List<String> sanitizeSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of("Continuer", "Passer");
        }
        return suggestions.stream()
                .filter(v -> v != null && !v.isBlank())
                .limit(3)
                .toList();
    }

    private Optional<String> detectUrl(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = URL_PATTERN.matcher(message);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
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

    private String nullToText(Object value) {
        return value == null ? "inconnu" : value.toString();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}