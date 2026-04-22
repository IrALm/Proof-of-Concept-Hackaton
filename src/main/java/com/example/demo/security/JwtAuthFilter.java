package com.example.demo.security;

import com.example.demo.supabaseAuth.SupabaseJwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT filter that runs once per request.
 *
 * <p>Extracts the Bearer token from the Authorization header, verifies it
 * using the existing {@link SupabaseJwtService} (JWKS-based RSA verification),
 * and injects the authenticated principal into the {@link SecurityContextHolder}.
 *
 * <p>Works transparently for both email/password and Google OAuth tokens —
 * both are standard Supabase JWTs with the same JWKS verification path.
 *
 * <p>If no token is present or verification fails, the request continues
 * unauthenticated. The authorization decision is left to {@code SecurityConfig}.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final SupabaseJwtService jwtService;

    public JwtAuthFilter(SupabaseJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token — continue unauthenticated (public endpoints will still pass)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.verifyJwt(authHeader);

            // Use the Supabase user UUID (sub claim) as the principal
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.getSubject(),    // userId (UUID)
                            null,                   // no credentials needed
                            List.of()               // roles — extend here if needed
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ResponseStatusException ignored) {
            // Invalid or expired token — SecurityContext stays empty.
            // Protected endpoints will return 401 via SecurityConfig.
        }

        chain.doFilter(request, response);
    }
}