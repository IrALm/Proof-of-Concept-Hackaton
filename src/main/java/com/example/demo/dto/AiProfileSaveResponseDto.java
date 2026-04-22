package com.example.demo.dto;

/**
 * Confirmation returned after preferences are persisted to Supabase.
 */
public record AiProfileSaveResponseDto(
        boolean success,
        String message
) {
}