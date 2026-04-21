package com.example.demo.service;

import com.example.demo.dto.UserProfileDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class UserProfileService {

    private final SupabaseDbService supabaseDbService;

    public UserProfileService(SupabaseDbService supabaseDbService) {
        this.supabaseDbService = supabaseDbService;
    }

    public UserProfileDto resolveUserProfile(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        try {
            return supabaseDbService.findAuthUser(authorizationHeader);
        } catch (RestClientResponseException ignored) {
            return null;
        }
    }
}
