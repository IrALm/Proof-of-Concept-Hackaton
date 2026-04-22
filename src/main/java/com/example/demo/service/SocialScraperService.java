package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Attempts to fetch a public social profile page (Instagram, TikTok, etc.)
 * and extract its visible text content for preference analysis.
 *
 * <p><strong>Limitations:</strong>
 * <ul>
 *   <li>Instagram frequently blocks server-side requests and returns a login wall.</li>
 *   <li>TikTok public pages are more accessible but may also be rate-limited.</li>
 *   <li>Any failure silently returns {@code Optional.empty()} — the caller must
 *       handle the fallback gracefully (continue with direct conversation).</li>
 * </ul>
 */
@Service
public class SocialScraperService {

    /** Maximum characters of extracted text passed to the AI for analysis. */
    private static final int MAX_CONTENT_LENGTH = 3000;

    /**
     * Minimum length of extracted text considered "meaningful".
     * Shorter results (e.g. login-wall pages, empty redirects) are discarded.
     */
    private static final int MIN_MEANINGFUL_LENGTH = 80;

    private final RestClient restClient;

    public SocialScraperService() {
        this.restClient = RestClient.builder()
                // Mimic a standard browser to reduce bot detection
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                                + "Chrome/124.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .defaultHeader("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                .build();
    }

    /**
     * Fetches {@code url} and returns stripped plain text, or {@code Optional.empty()}
     * if the page is inaccessible, blocked, or contains too little useful content.
     *
     * @param url absolute HTTP/HTTPS URL of a public social profile
     * @return extracted text limited to {@value MAX_CONTENT_LENGTH} characters,
     *         or {@code Optional.empty()} on any failure
     */
    public Optional<String> extractPublicContent(String url) {
        try {
            String html = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                return Optional.empty();
            }

            String text = stripHtml(html);

            if (text.length() < MIN_MEANINGFUL_LENGTH) {
                // Likely a login redirect or empty shell page
                return Optional.empty();
            }

            String truncated = text.length() > MAX_CONTENT_LENGTH
                    ? text.substring(0, MAX_CONTENT_LENGTH)
                    : text;

            return Optional.of(truncated);

        } catch (Exception e) {
            // Network error, timeout, HTTP 4xx/5xx — all treated as "not found"
            return Optional.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTML STRIPPING
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Removes script/style blocks, HTML tags, and common HTML entities,
     * then collapses whitespace into single spaces.
     */
    private String stripHtml(String html) {
        return html
                // Remove script and style blocks entirely
                .replaceAll("(?si)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?si)<style[^>]*>.*?</style>", " ")
                // Remove all remaining HTML tags
                .replaceAll("<[^>]+>", " ")
                // Decode common HTML entities
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replaceAll("&#?[a-zA-Z0-9]+;", " ")
                // Collapse whitespace
                .replaceAll("\\s+", " ")
                .trim();
    }
}