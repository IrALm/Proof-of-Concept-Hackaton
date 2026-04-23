package com.example.demo.admin.controller;

import com.example.demo.admin.dto.ImportResult;
import com.example.demo.admin.service.HotelImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("${app.admin.base-path}/hotels")
public class AdminHotelController {

    private final HotelImportService importService;

    public AdminHotelController(HotelImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/import")
    public String showImportForm() {
        return "admin/hotels-import";
    }

    @PostMapping("/import")
    public String handleImport(@RequestParam("file") MultipartFile file, Model model) {

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "Aucun fichier sélectionné.");
            return "admin/hotels-import";
        }

        try {
            ImportResult result = importService.importHotelsCsv(file);
            model.addAttribute("result", result);
        } catch (Exception e) {
            model.addAttribute("error", "Erreur pendant l'import : " + e.getMessage());
        }

        return "admin/hotels-import";
    }
}
