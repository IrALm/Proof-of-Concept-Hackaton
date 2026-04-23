package com.example.demo.admin.service;

import com.example.demo.admin.dto.ImportResult;
import com.example.demo.admin.dto.RestaurantCsvRow;
import com.example.demo.service.SupabaseDbService;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Orchestration de l'import CSV restaurants :
 *   1. Parse du CSV via OpenCSV
 *   2. Nettoyage + enrichissement (photo Pexels, rating/review mockés selon award)
 *   3. Insertion en batchs via PostgREST (Supabase)
 *
 * <p>Réplique fidèlement la logique du script Python {@code import_restaurants.py}.
 */
@Service
public class RestaurantImportService {

    private static final int BATCH_SIZE = 50;

    private final PexelsPhotoService pexelsService;
    private final SupabaseDbService dbService;
    private final Random random = new Random();

    public RestaurantImportService(PexelsPhotoService pexelsService,
                                   SupabaseDbService dbService) {
        this.pexelsService = pexelsService;
        this.dbService = dbService;
    }

    public ImportResult importCsv(MultipartFile file) throws Exception {
        ImportResult result = new ImportResult();

        List<RestaurantCsvRow> rows;
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            rows = new CsvToBeanBuilder<RestaurantCsvRow>(reader)
                    .withType(RestaurantCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(false)
                    .build()
                    .parse();
        }

        // Filtre lignes sans nom (comme le script Python)
        List<RestaurantCsvRow> validRows = new ArrayList<>();
        for (RestaurantCsvRow r : rows) {
            if (r.getName() != null && !r.getName().isBlank()) {
                validRows.add(r);
            }
        }

        result.setTotalParsed(validRows.size());

        // ── Batch insert ───────────────────────────────────────────────
        for (int i = 0; i < validRows.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, validRows.size());
            List<RestaurantCsvRow> batch = validRows.subList(i, end);

            List<Map<String, Object>> payload = new ArrayList<>(batch.size());
            for (RestaurantCsvRow row : batch) {
                payload.add(toPayload(row));
            }

            try {
                dbService.insertRestaurantsBatch(payload);
                result.addSuccess(payload.size());
            } catch (Exception e) {
                result.addError(payload.size(),
                        "Batch " + (i / BATCH_SIZE + 1) + " : " + e.getMessage());
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Transformation CSV → payload PostgREST
    // ─────────────────────────────────────────────────────────────────

    private Map<String, Object> toPayload(RestaurantCsvRow row) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", clean(row.getName()));
        p.put("address", clean(row.getAddress()));
        p.put("location", clean(row.getLocation()));
        p.put("price", clean(row.getPrice()));
        p.put("cuisine", clean(row.getCuisine()));
        p.put("longitude", toDouble(row.getLongitude()));
        p.put("latitude", toDouble(row.getLatitude()));
        p.put("phone_number", clean(row.getPhoneNumber()));
        p.put("url_michelin", clean(row.getUrl()));
        p.put("website_url", clean(row.getWebsiteUrl()));
        p.put("award", clean(row.getAward()));
        p.put("green_star", toBool(row.getGreenStar()));
        p.put("facilities_services", clean(row.getFacilitiesAndServices()));
        p.put("description", clean(row.getDescription()));

        p.put("photo_url", pexelsService.getRestaurantPhoto(row.getCuisine(), row.getLocation()));
        p.put("rating", generateRating(row.getAward()));
        p.put("review_count", generateReviewCount(row.getAward()));

        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    // Génération du rating / review_count selon l'award (comme le script Python)
    // ─────────────────────────────────────────────────────────────────

    private double generateRating(String award) {
        String a = award == null ? "" : award;
        if (a.contains("3")) return round1(4.7 + random.nextDouble() * 0.3);
        if (a.contains("2")) return round1(4.4 + random.nextDouble() * 0.4);
        if (a.contains("1")) return round1(4.0 + random.nextDouble() * 0.6);
        return round1(3.5 + random.nextDouble() * 0.8);
    }

    private int generateReviewCount(String award) {
        String a = award == null ? "" : award;
        if (a.contains("3")) return 800 + random.nextInt(2201);
        if (a.contains("2")) return 500 + random.nextInt(1501);
        if (a.contains("1")) return 200 + random.nextInt(1301);
        return 50 + random.nextInt(751);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers de conversion
    // ─────────────────────────────────────────────────────────────────

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double toDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean toBool(String s) {
        if (s == null || s.isBlank()) return false;
        String v = s.trim();
        if (v.equalsIgnoreCase("true")) return true;
        try { return Integer.parseInt(v) != 0; }
        catch (NumberFormatException e) { return false; }
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
