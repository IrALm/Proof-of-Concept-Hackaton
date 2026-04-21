package com.example.demo.controller;

import com.example.demo.dto.RestoSearchRDto;
import com.example.demo.dto.RestoSearchResultDto;
import com.example.demo.service.RestoSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestoSearchController {

    private final RestoSearchService service;

    public RestoSearchController(RestoSearchService service) {
        this.service = service;
    }

    // ─────────────────────────────────────────────
    // 🔎 SEARCH + 🏠 HOME FEED (AUTO MODE)
    // ─────────────────────────────────────────────
    @PostMapping("/search")
    public ResponseEntity<List<RestoSearchResultDto>> search(
            @Valid @RequestBody RestoSearchRDto dto
    ) {

        List<RestoSearchResultDto> results = service.search(dto);

        return ResponseEntity
                .ok()
                .body(results);
    }
}