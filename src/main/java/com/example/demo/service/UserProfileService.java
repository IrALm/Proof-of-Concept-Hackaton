package com.example.demo.service;

import com.example.demo.dto.UserProfileDto;
import com.example.demo.supabaseAuth.SupabaseJwtService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final SupabaseJwtService supabaseJwtService;
    private final SupabaseDbService supabaseDbService;

    public UserProfileService(SupabaseJwtService supabaseJwtService, SupabaseDbService supabaseDbService) {
        this.supabaseJwtService = supabaseJwtService;
        this.supabaseDbService = supabaseDbService;
    }

    public UserProfileDto resolveUserProfile(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        if (!supabaseJwtService.looksLikeUserJwt(authorizationHeader)) {
            return null;
        }

        try {
            Claims claims = supabaseJwtService.verifyJwt(authorizationHeader);
            String userId = claims.getSubject();
            UserProfileDto userProfile = supabaseDbService.findUserById(userId);

            if (userProfile != null) {
                return userProfile;
            }

            return new UserProfileDto(
                    userId,
                    claims.get("email", String.class),
                    null
            );
        } catch (ResponseStatusException ignored) {
            return null;
        }
    }
}
