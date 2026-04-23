package com.example.demo.admin.controller;

import com.example.demo.admin.dto.ImportResult;
import com.example.demo.admin.service.RestaurantImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("${app.admin.base-path}/restaurants")
public class AdminRestaurantController {

    private final RestaurantImportService importService;

    public AdminRestaurantController(RestaurantImportService importService) {
        this.importService = importService;
    }

    // ─── GET : affiche le formulaire ──────────────────────────────────
    @GetMapping("/import")
    public String showImportForm() {
        return "admin/restaurants-import";
    }

    // ─── POST : traite l'upload ───────────────────────────────────────
    @PostMapping("/import")
    public String handleImport(@RequestParam("file") MultipartFile file, Model model) {

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Aucun fichier sélectionné.");
            return "admin/restaurants-import";
        }

        try {
            ImportResult result = importService.importCsv(file);
            model.addAttribute("result", result);
        } catch (Exception e) {
            model.addAttribute("error", "Erreur pendant l'import : " + e.getMessage());
        }

        return "admin/restaurants-import";
    }
}
