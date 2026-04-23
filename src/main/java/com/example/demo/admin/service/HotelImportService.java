package com.example.demo.admin.service;

import com.example.demo.admin.dto.HotelBookingCsvRow;
import com.example.demo.admin.dto.ImportResult;
import com.example.demo.service.SupabaseDbService;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Import CSV hôtels + bookings :
 * - importHotelsCsv → import simple hôtels (1 à 1)
 * - importCsv → import bookings + création hôtels (1 à 1)
 */
@Service
public class HotelImportService {

    private final PexelsPhotoService pexelsService;
    private final SupabaseDbService dbService;
    private final Random random = new Random();

    public HotelImportService(PexelsPhotoService pexelsService,
                              SupabaseDbService dbService) {
        this.pexelsService = pexelsService;
        this.dbService = dbService;
    }

    // ============================================================
    // IMPORT SIMPLE HOTELS CSV (INSERT 1 À 1)
    // ============================================================

    public ImportResult importHotelsCsv(MultipartFile file) throws Exception {

        ImportResult result = new ImportResult();

        List<Map<String, String>> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> csv = reader.readAll();

            for (int i = 1; i < csv.size(); i++) {
                String[] line = csv.get(i);
                if (line.length < 7) continue;

                Map<String, String> row = new HashMap<>();
                row.put("id", safe(line, 0));
                row.put("name", safe(line, 1));
                row.put("country", safe(line, 2));
                row.put("market_segment", safe(line, 3));
                row.put("photo_url", safe(line, 4));
                row.put("rating", safe(line, 5));
                row.put("review_count", safe(line, 6));
                row.put("created_at", safe(line, 7));

                rows.add(row);
            }
        }

        result.setTotalParsed(rows.size());

        // 🔥 DÉDUP PAR NOM
        Map<String, Map<String, Object>> uniques = new LinkedHashMap<>();

        for (Map<String, String> r : rows) {

            String name = clean(r.get("name"));
            if (name == null) continue;

            uniques.putIfAbsent(name, Map.of(
                    "name", name,
                    "country", clean(r.get("country")),
                    "market_segment", clean(r.get("market_segment")),
                    "photo_url", clean(r.get("photo_url")),
                    "rating", toDouble(r.get("rating")),
                    "review_count", toInt(r.get("review_count"))
            ));
        }

        // ============================================================
        // 🚀 INSERT 1 À 1 (MODIF PRINCIPALE)
        // ============================================================

        int success = 0;
        int errors = 0;

        for (Map<String, Object> hotel : uniques.values()) {
            try {
                dbService.insertHotel(hotel); // 👈 INSERT INDIVIDUEL
                success++;
            } catch (Exception e) {
                errors++;
                System.out.println("Hotel insert error: " + e.getMessage());
            }
        }

        result.addSuccess(success);
        result.addError(errors, "Hotels inserted with individual mode");

        return result;
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private static String safe(String[] line, int i) {
        return (line != null && line.length > i) ? line[i] : null;
    }

    private static String clean(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Integer toInt(String s) {
        try { return (s == null) ? null : (int) Double.parseDouble(s.trim()); }
        catch (Exception e) { return null; }
    }

    private static Double toDouble(String s) {
        try { return (s == null) ? null : Double.parseDouble(s.trim()); }
        catch (Exception e) { return null; }
    }

    private static boolean toBool(String s) {
        if (s == null) return false;
        return s.equals("1") || s.equalsIgnoreCase("true");
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}