package com.example.demo.security;

import com.example.demo.supabaseAuth.SupabaseJwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT filter qui lit le token depuis :
 *   1. Le header Authorization: Bearer xxx (API REST / front Vite)
 *   2. Le cookie admin_token (back-office Thymeleaf)
 *
 * <p>Après vérification JWKS, si le {@code sub} correspond à l'UUID de l'admin
 * déclaré dans {@code app.admin.user-id}, le principal reçoit {@code ROLE_ADMIN}.
 * Sinon, aucune authority — le principal est juste authentifié en tant qu'utilisateur standard.
 *
 * <p>Si aucun token n'est présent ou si la vérification échoue, la requête continue
 * non authentifiée. La décision d'autorisation est laissée à {@code SecurityConfig}.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_COOKIE_NAME = "admin_token";

    private final SupabaseJwtService jwtService;
    private final String adminUserId;

    public JwtAuthFilter(SupabaseJwtService jwtService,
                         @Value("${app.admin.user-id:}") String adminUserId) {
        this.jwtService = jwtService;
        this.adminUserId = adminUserId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // On reformate en "Bearer xxx" car verifyJwt attend un header Authorization
            Claims claims = jwtService.verifyJwt("Bearer " + token);
            String sub = claims.getSubject();

            // ── Rôles : ADMIN si le sub matche l'UUID admin configuré ──
            List<SimpleGrantedAuthority> authorities = isAdmin(sub)
                    ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    : List.of();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(sub, null, authorities);

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ResponseStatusException ignored) {
            // Token invalide / expiré → SecurityContext reste vide.
            // Les endpoints protégés renverront 401/403 via SecurityConfig.
        }

        chain.doFilter(request, response);
    }

    /**
     * Extrait le JWT depuis :
     *   1. Header Authorization: Bearer xxx  (prioritaire — API)
     *   2. Cookie admin_token                (fallback — back-office)
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (ADMIN_COOKIE_NAME.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return (value != null && !value.isBlank()) ? value : null;
                }
            }
        }


        return null;
    }

    private boolean isAdmin(String sub) {
        return adminUserId != null
                && !adminUserId.isBlank()
                && adminUserId.equals(sub);
    }
}
