package com.example.demo.supabaseAuth;

import com.example.demo.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SupabaseJwtService {

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final AtomicReference<JsonNode> jwksCache = new AtomicReference<>();

    public SupabaseJwtService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(appProperties.getSupabase().getUrl())
                .defaultHeader("apikey", appProperties.getSupabase().getKey())
                .build();
    }

    public JsonNode getJwks() {
        JsonNode cached = jwksCache.get();
        if (cached != null) {
            return cached;
        }

        String body = restClient.get()
                .uri("/auth/v1/.well-known/jwks.json")
                .retrieve()
                .body(String.class);

        try {
            JsonNode jwks = objectMapper.readTree(body);
            jwksCache.set(jwks);
            return jwks;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse Supabase JWKS", e);
        }
    }



    public Claims verifyJwt(String authorizationHeader) {

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }

        try {
            String token = extractBearerToken(authorizationHeader);

            if (!looksLikeJwt(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header does not contain a JWT access token");
            }

            String kid = extractKid(token);
            JsonNode jwks = getJwks();
            JsonNode keys = jwks.get("keys");

            if (keys == null || !keys.isArray() || keys.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No valid signing key found");
            }

            JsonNode matchingKey = null;
            Iterator<JsonNode> iterator = keys.elements();

            while (iterator.hasNext()) {
                JsonNode key = iterator.next();
                if (kid != null && kid.equals(key.path("kid").asText())) {
                    matchingKey = key;
                    break;
                }
            }

            if (matchingKey == null) {
                matchingKey = keys.get(0);
            }

            // 🔥 PARSING RSA (remplace JwksKeyParser)
            RSAPublicKey publicKey = buildRsaPublicKey(matchingKey);

            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireAudience("authenticated")
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT invalid: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    public boolean looksLikeUserJwt(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        try {
            return looksLikeJwt(extractBearerToken(authorizationHeader));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String extractKid(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                return null;
            }

            byte[] decoded = java.util.Base64.getUrlDecoder().decode(chunks[0]);
            JsonNode header = objectMapper.readTree(decoded);
            return header.path("kid").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorizationHeader.trim();
        }

        return authorizationHeader.substring(7).trim();
    }

    private boolean looksLikeJwt(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.split("\\.");
        return parts.length == 3;
    }

    private RSAPublicKey buildRsaPublicKey(JsonNode keyNode) throws Exception {

        String n = keyNode.get("n").asText(); // modulus
        String e = keyNode.get("e").asText(); // exponent

        byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }
}

