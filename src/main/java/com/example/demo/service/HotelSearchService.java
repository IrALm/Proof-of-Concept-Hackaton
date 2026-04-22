package com.example.demo.service;

import com.example.demo.dto.HotelSearchRequestDto;
import com.example.demo.dto.HotelSearchResultDto;
import com.example.demo.repository.HotelRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HotelSearchService {

    private final HotelRepository hotelRepository;

    public HotelSearchService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public List<HotelSearchResultDto> search(HotelSearchRequestDto dto) {
        boolean hasFilters = hasAnyFilter(dto);
        List<Object[]> results;

        if (isNewFeedRequest(dto)) {
            results = hotelRepository.findNewHotels(safeLimit(dto), safeOffset(dto));
        } else if (!hasFilters) {
            results = hotelRepository.findHomeFeed(safeLimit(dto), safeOffset(dto));
        } else {
            results = hotelRepository.searchHotels(
                    dto.name(),
                    dto.country(),
                    dto.marketSegment(),
                    dto.minRating(),
                    dto.maxAdr(),
                    dto.customerType(),
                    dto.isRepeatedGuest(),
                    dto.familyFriendly(),
                    dto.businessFriendly(),
                    dto.lowCancellation(),
                    normalizeSortKey(dto),
                    safeLimit(dto),
                    safeOffset(dto)
            );
        }

        return results.stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<HotelSearchResultDto> homeFeed(int limit, int offset) {
        return hotelRepository.findHomeFeed(limit, offset).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<HotelSearchResultDto> newHotels(int limit, int offset) {
        return hotelRepository.findNewHotels(limit, offset).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<HotelSearchResultDto> familyRecommendations(int limit, int offset) {
        return search(HotelSearchRequestDto.builder()
                .familyFriendly(true)
                .sortBy("family")
                .limit(limit)
                .offset(offset)
                .build());
    }

    public List<HotelSearchResultDto> businessRecommendations(int limit, int offset) {
        return search(HotelSearchRequestDto.builder()
                .businessFriendly(true)
                .sortBy("business")
                .limit(limit)
                .offset(offset)
                .build());
    }

    private boolean hasAnyFilter(HotelSearchRequestDto dto) {
        return dto.name() != null
                || dto.country() != null
                || dto.marketSegment() != null
                || dto.minRating() != null
                || dto.maxAdr() != null
                || dto.customerType() != null
                || dto.isRepeatedGuest() != null
                || dto.familyFriendly() != null
                || dto.businessFriendly() != null
                || dto.lowCancellation() != null;
    }

    private int safeLimit(HotelSearchRequestDto dto) {
        int requested = dto.limit() != null ? dto.limit() : (dto.size() != null ? dto.size() : 10);
        return Math.max(1, Math.min(requested, 20));
    }

    private int safeOffset(HotelSearchRequestDto dto) {
        if (dto.offset() != null) {
            return Math.max(0, dto.offset());
        }
        if (dto.page() != null && dto.size() != null) {
            return Math.max(0, dto.page() * dto.size());
        }
        return 0;
    }

    private boolean isNewFeedRequest(HotelSearchRequestDto dto) {
        return Boolean.TRUE.equals(dto.newOnly())
                || "new".equalsIgnoreCase(dto.sortBy())
                || "recent".equalsIgnoreCase(dto.sortBy());
    }

    private String normalizeSortKey(HotelSearchRequestDto dto) {
        String sortBy = dto.sortBy();
        String sortDirection = dto.sortDirection();

        if (sortBy == null || sortBy.isBlank()) {
            return "score";
        }

        String normalized = sortBy.trim().toLowerCase();
        String direction = sortDirection == null ? "desc" : sortDirection.trim().toLowerCase();

        return switch (normalized) {
            case "new", "recent" -> "new";
            case "rating" -> "rating";
            case "review_count", "reviews", "popular" -> "review_count";
            case "family", "family_score" -> "family";
            case "business", "business_score" -> "business";
            case "cancellation", "cancellation_low", "reliability" -> "cancellation_low";
            case "adr", "price", "budget" -> "asc".equals(direction) ? "adr_low" : "adr_high";
            default -> "score";
        };
    }

    private HotelSearchResultDto mapToDto(Object[] row) {
        LocalDateTime createdAt = null;
        if (row[7] instanceof Timestamp timestamp) {
            createdAt = timestamp.toLocalDateTime();
        } else if (row[7] instanceof LocalDateTime value) {
            createdAt = value;
        }

        return new HotelSearchResultDto(
                row[0] != null ? ((Number) row[0]).intValue() : null,
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                row[5] != null ? ((Number) row[5]).doubleValue() : null,
                row[6] != null ? ((Number) row[6]).intValue() : null,
                createdAt,
                row[8] != null ? ((Number) row[8]).doubleValue() : null,
                row[9] != null ? ((Number) row[9]).intValue() : null,
                row[10] != null ? ((Number) row[10]).doubleValue() : null,
                row[11] != null ? ((Number) row[11]).doubleValue() : null,
                row[12] != null ? ((Number) row[12]).doubleValue() : null,
                row[13] != null ? ((Number) row[13]).doubleValue() : null,
                row[14] != null ? ((Number) row[14]).doubleValue() : null,
                row[15] != null ? ((Number) row[15]).doubleValue() : null
        );
    }
}
