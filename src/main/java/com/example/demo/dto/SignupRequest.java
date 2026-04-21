package com.example.demo.dto;

public record SignupRequest(
        String email,
        String password,
        String fullName
) {}
