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
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF désactivé (API stateless JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // IMPORTANT : preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // AUTH ADMIN
                        .requestMatchers(adminBasePath + "/login").permitAll()
                        .requestMatchers(adminBasePath + "/logout").permitAll()

                        // ADMIN DASHBOARD
                        .requestMatchers(adminBasePath + "/**").permitAll()

                        // API REST
                        .anyRequest().permitAll()
                )

                // JWT filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Origins autorisées (prod + local + Render)
        List<String> allowedOrigins = appProperties.getCors().getAllowedOrigins();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:8000",
                "https://*.onrender.com"
        ));

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            config.setAllowedOrigins(allowedOrigins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        // utile pour JWT si besoin côté front
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}