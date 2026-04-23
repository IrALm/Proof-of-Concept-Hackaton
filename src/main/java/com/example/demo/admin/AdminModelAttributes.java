package com.example.demo.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Rend le {@code basePath} admin disponible dans tous les templates Thymeleaf
 * du back-office (ex: pour les {@code action="${basePath}/restaurants/import"}).
 *
 * Scopé aux contrôleurs du package {@code com.example.demo.admin.controller}
 * pour ne pas polluer le reste de l'app.
 */
@ControllerAdvice(basePackages = "com.example.demo.admin.controller")
public class AdminModelAttributes {

    private final String basePath;

    public AdminModelAttributes(@Value("${app.admin.base-path}") String basePath) {
        this.basePath = basePath;
    }

    @ModelAttribute
    public void injectCommonAttributes(Model model) {
        model.addAttribute("basePath", basePath);
    }
}
