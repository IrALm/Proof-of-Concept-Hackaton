package com.example.demo.security;

import com.example.demo.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <p><strong>Auth strategy:</strong> fully stateless — no sessions, no cookies.
 * Every request is authenticated via the JWT Bearer token verified by
 * {@link JwtAuthFilter}. This covers both email/password and Google OAuth flows
 * (both produce standard Supabase JWTs verified by the same JWKS endpoint).
 *
 * <p><strong>Google OAuth setup</strong> is handled entirely on the Supabase side:
 * <ol>
 *   <li>Supabase Dashboard → Authentication → Providers → Google → Enable</li>
 *   <li>Paste your Google OAuth Client ID + Secret (from Google Cloud Console)</li>
 *   <li>Add {@code https://<your-supabase-project>.supabase.co/auth/v1/callback}
 *       as an Authorized Redirect URI in Google Cloud Console</li>
 *   <li>Frontend calls {@code supabase.auth.signInWithOAuth({ provider: 'google' })}</li>
 *   <li>Supabase returns a JWT — Spring verifies it identically to email/password tokens</li>
 * </ol>
 * No Spring OAuth2 client dependency needed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AppProperties appProperties;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AppProperties appProperties) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.appProperties = appProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS ────────────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── CSRF disabled (stateless REST API) ──────────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── No sessions ─────────────────────────────────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules ─────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Preflight OPTIONS requests must always pass
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public auth endpoints
                        .requestMatchers(
                                "/**"
                        ).permitAll()

                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                // ── JWT filter before Spring's default auth filter ───────────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // CORS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * CORS configuration sourced from {@code AppProperties}.
     * Allowed origins are set in {@code application.yml} under {@code app.cors.allowed-origins}
     * so they can differ between local, staging and production environments.
     *
     * <pre>
     * # application.yml
     * app:
     *   cors:
     *     allowed-origins:
     *       - http://localhost:3000
     *       - https://your-frontend.vercel.app
     * </pre>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origins from config — never hard-code in production
        List<String> allowedOrigins = appProperties.getCors().getAllowedOrigins();
        config.setAllowedOrigins(allowedOrigins != null && !allowedOrigins.isEmpty()
                ? allowedOrigins
                : List.of("http://localhost:5173") // safe fallback for local dev
        );

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow all headers (Authorization, Content-Type, etc.)
        config.setAllowedHeaders(List.of("*"));

        // Required if the frontend sends cookies or Authorization header with credentials
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}