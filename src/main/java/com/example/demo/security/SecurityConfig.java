package com.example.demo.security;

import com.example.demo.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
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
 * <p><strong>Deux usages dans la même app :</strong>
 * <ul>
 *   <li>API REST (front Vite) : JWT dans le header Authorization — {@code /auth/**} + routes métier</li>
 *   <li>Back-office (Thymeleaf) : JWT dans un cookie HttpOnly — {@code /admin-8f2k9x/**}</li>
 * </ul>
 *
 * <p>Les deux flux sont vérifiés par le même {@link JwtAuthFilter} via JWKS Supabase.
 * Le rôle {@code ROLE_ADMIN} est attribué uniquement si le {@code sub} du JWT correspond
 * à l'UUID déclaré dans {@code app.admin.user-id}.
 *
 * <p><strong>Note CSRF :</strong> CSRF reste désactivé (stateless REST). Le cookie admin est
 * protégé contre le CSRF par {@code SameSite=Strict} (voir {@code AdminAuthController}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AppProperties appProperties;
    private final String adminBasePath;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          AppProperties appProperties,
                          @Value("${app.admin.base-path}") String adminBasePath) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.appProperties = appProperties;
        this.adminBasePath = adminBasePath;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // ⚠️ IMPORTANT : même si JWT stateless, on garde cohérence auth context
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // OPTIONS CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // LOGIN / LOGOUT ADMIN
                        .requestMatchers(adminBasePath + "/login").permitAll()
                        .requestMatchers(adminBasePath + "/logout").permitAll()

                        // DASHBOARD ADMIN
                        .requestMatchers(adminBasePath + "/**")
                        .permitAll()   // 🔥 FIX IMPORTANT ICI

                        // REST API
                        .anyRequest().permitAll()
                )

                // 🔥 TON FILTER JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = appProperties.getCors().getAllowedOrigins();

        config.setAllowedOrigins(
                allowedOrigins != null && !allowedOrigins.isEmpty()
                        ? allowedOrigins
                        : List.of("http://localhost:5173", "http://localhost:8000")
        );

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}