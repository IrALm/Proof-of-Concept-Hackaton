package com.example.demo.dto;

public record UserProfileDto(
        String id,
        String email,
        String fullName,
        java.util.Map<String, Object> preferences) {
}
