package com.example.demo.config;

import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Getter
@Builder
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Supabase supabase;
    private Groq groq;
    private Pexels pexels;
    private Api api;

    @Getter
    @Builder
    public static class Supabase {
        private String url;
        private String key;
        private String anonKey;
        private String serviceKey;
        // getters/setters
    }

    @Getter
    @Builder
    public static class Groq {
        private String apiKey;
        // getters/setters
    }

    @Getter
    @Builder
    public static class Pexels {
        private String apiKey;
        // getters/setters
    }

    @Getter
    @Builder
    public static class Api {
        private String url;
        // getters/setters
    }

    // getters/setters
}

