package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Supabase supabase;
    private Groq groq;
    private Pexels pexels;
    private Api api;

    @Getter
    @Setter
    public static class Supabase {
        private String url;
        private String key;
        private String anonKey;
        private String serviceKey;
    }

    @Getter
    @Setter
    public static class Groq {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Pexels {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Api {
        private String url;
    }
}

