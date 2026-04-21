package com.example.demo.service;

import com.example.demo.dto.RestoSearchRDto;
import com.example.demo.dto.RestoSearchResultDto;
import com.example.demo.repository.RestoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class RestoSearchService {

    private final RestoRepository repo;

    public RestoSearchService(RestoRepository repo) {
        this.repo = repo;
    }

    public List<RestoSearchResultDto> search(RestoSearchRDto dto) {

        boolean hasFilters = hasAnyFilter(dto);

        List<Object[]> results;

        if (!hasFilters) {
            // 🏠 HOME FEED MODE
            results = repo.findHomeFeed(
                    safeLimit(dto),
                    safeOffset(dto)
            );
        } else {
            // 🔎 SEARCH MODE
            results = repo.searchRestaurants(
                    dto.name(),
                    dto.location(),
                    dto.cuisine(),
                    dto.greenStar(),
                    dto.award(),
                    dto.price(),
                    dto.latitude(),
                    dto.longitude(),
                    dto.radius(),
                    safeLimit(dto),
                    safeOffset(dto)
            );
        }

        return results.stream()
                .map(this::mapToDto)
                .toList();
    }

    // ─────────────────────────────────────
    // DETECT IF ANY FILTER IS USED
    // ─────────────────────────────────────
    private boolean hasAnyFilter(RestoSearchRDto dto) {
        return dto.name() != null
                || dto.location() != null
                || dto.cuisine() != null
                || dto.greenStar() != null
                || dto.award() != null
                || dto.price() != null
                || dto.latitude() != null
                || dto.longitude() != null
                || dto.radius() != null;
    }

    // ─────────────────────────────────────
    // SAFE PAGINATION DEFAULTS
    // ─────────────────────────────────────
    private int safeLimit(RestoSearchRDto dto) {
        return dto.limit() != null ? dto.limit()
                : (dto.size() != null ? dto.size() : 10);
    }

    private int safeOffset(RestoSearchRDto dto) {
        if (dto.offset() != null) return dto.offset();
        if (dto.page() != null && dto.size() != null) {
            return dto.page() * dto.size();
        }
        return 0;
    }

    // ─────────────────────────────────────
    // MAPPING SQL → DTO
    // ─────────────────────────────────────
    private RestoSearchResultDto mapToDto(Object[] row) {
        return new RestoSearchResultDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                row[5] != null ? ((Number) row[5]).doubleValue() : null,
                row[6] != null ? ((Number) row[6]).intValue() : null,
                (String) row[7],
                (String) row[8],
                (Boolean) row[9],
                row[10] != null ? ((Number) row[10]).doubleValue() : null,
                row[11] != null ? ((Number) row[11]).doubleValue() : null,
                row[12] != null ? ((Number) row[12]).doubleValue() : null
        );
    }
}