package com.example.demo.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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

    // Dans AppProperties.java, ajouter :
    private Cors cors = new Cors();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}

