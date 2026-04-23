package com.example.demo.admin.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.supabaseAuth.SupabaseAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("${app.admin.base-path}")
public class AdminAuthController {

    private static final String COOKIE_NAME = "admin_token";

    private final SupabaseAuthService authService;
    private final String adminUserId;
    private final boolean cookieSecure;
    private final String basePath;
    private final String cookieSameSite;
    private final long cookieMaxAge;

    public AdminAuthController(
            SupabaseAuthService authService,
            @Value("${app.admin.user-id}") String adminUserId,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite,
            @Value("${app.cookie.max-age:1209600}") long cookieMaxAge,
            @Value("${app.admin.base-path}") String basePath
    ) {
        this.authService = authService;
        this.adminUserId = adminUserId;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieMaxAge = cookieMaxAge;
        this.basePath = basePath;
    }

    // ─────────────────────────────────────────────
    @GetMapping("/login")
    public String showLoginForm() {
        return "admin/login";
    }

    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpServletResponse response,
                          Model model) {

        JsonNode data;
        try {
            data = authService.login(new LoginRequest(email, password));
        } catch (Exception e) {
            model.addAttribute("error", "Identifiants invalides.");
            return "admin/login";
        }

        if (data == null || !data.has("access_token")) {
            model.addAttribute("error", "Identifiants invalides.");
            return "admin/login";
        }

        String userId = data.path("user").path("id").asText(null);
        if (userId == null || !userId.equals(adminUserId)) {
            model.addAttribute("error", "Accès refusé.");
            return "admin/login";
        }

        String accessToken = data.get("access_token").asText();

        // 🔥 IMPORTANT FIX COOKIE CHROME
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(cookieMaxAge)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return "redirect:" + basePath;
    }

    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public String doLogout(@CookieValue(value = COOKIE_NAME, required = false) String token,
                           HttpServletResponse response) {

        try {
            if (token != null && !token.isBlank()) {
                authService.logout(token);
            }
        } catch (Exception ignored) {}

        ResponseCookie clearCookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return "redirect:" + basePath + "/login";
    }
}