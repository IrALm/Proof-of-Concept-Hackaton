package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import com.example.demo.supabaseAuth.SupabaseAuthService;
import com.example.demo.service.SupabaseDbService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Endpoints d'authentification Supabase")
public class AuthController {

    private final SupabaseAuthService authService;
    private final SupabaseDbService dbService;

    public AuthController(SupabaseAuthService authService, SupabaseDbService dbService) {
        this.authService = authService;
        this.dbService = dbService;
    }

    // ─────────────────────────────
    // SIGNUP
    // ─────────────────────────────
    @Operation(
            summary = "Créer un compte utilisateur",
            description = "Crée un utilisateur via Supabase Auth et l'enregistre dans la table users"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Erreur de création utilisateur"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest body) {

        JsonNode data = authService.signup(body);

        String userId = data.path("user").path("id").asText(null);
        String accessToken = data.path("access_token").asText(null);

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User creation failed");
        }

        try {
            dbService.insertUser(
                    userId,
                    body.email(),
                    body.fullName()
            );
        } catch (Exception e) {
            System.out.println("Warning: user created in auth but not inserted in DB: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "message", "Compte créé avec succès",
                "user_id", userId,
                "email", body.email(),
                "access_token", accessToken
        ));
    }

    // ─────────────────────────────
    // LOGIN
    // ─────────────────────────────
    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur via Supabase et retourne un JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connexion réussie"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {

        JsonNode data = authService.login(body);

        if (!data.has("access_token")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, data.toString());
        }

        return ResponseEntity.ok(Map.of(
                "access_token", data.get("access_token").asText(),
                "refresh_token", data.get("refresh_token").asText(),
                "expires_in", data.get("expires_in").asInt(),
                "user", Map.of(
                        "id", data.path("user").path("id").asText(),
                        "email", data.path("user").path("email").asText()
                )
        ));
    }

    // ─────────────────────────────
    // LOGOUT
    // ─────────────────────────────
    @Operation(
            summary = "Déconnexion utilisateur",
            description = "Invalide la session utilisateur côté Supabase"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "401", description = "Token manquant ou invalide")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        if (authorization == null || authorization.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String token = authorization.replace("Bearer ", "").trim();

        authService.logout(token);

        return ResponseEntity.ok(Map.of(
                "message", "Déconnexion réussie"
        ));
    }
}