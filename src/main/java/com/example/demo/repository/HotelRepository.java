package com.example.demo.repository;

import com.example.demo.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

    @Query(value = """
        SELECT
            h.id,
            h.name,
            h.country,
            h.market_segment,
            h.photo_url,
            h.rating,
            h.review_count,
            h.created_at,
            COALESCE(b.avg_adr, 0),
            COALESCE(b.booking_count, 0),
            COALESCE(b.cancellation_rate, 0),
            COALESCE(b.repeated_guest_rate, 0),
            COALESCE(b.family_score, 0),
            COALESCE(b.business_score, 0),
            COALESCE(b.avg_special_requests, 0),
            (
                COALESCE(h.rating, 0) * 3
                + LN(GREATEST(COALESCE(h.review_count, 0), 1)) * 1.5
                + COALESCE(b.repeated_guest_rate, 0) * 2
                + COALESCE(b.family_score, 0)
                + COALESCE(b.business_score, 0)
                - COALESCE(b.cancellation_rate, 0) * 2.5
                - COALESCE(b.avg_adr, 0) / 250.0
            ) AS score
        FROM hotels h
        LEFT JOIN (
            SELECT
                lower(hb.hotel) AS hotel_key,
                AVG(COALESCE(hb.adr, 0)) AS avg_adr,
                COUNT(*)::int AS booking_count,
                AVG(CASE WHEN hb.is_canceled THEN 1.0 ELSE 0.0 END) AS cancellation_rate,
                AVG(CASE WHEN hb.is_repeated_guest THEN 1.0 ELSE 0.0 END) AS repeated_guest_rate,
                AVG(CASE WHEN COALESCE(hb.children, 0) + COALESCE(hb.babies, 0) > 0 THEN 1.0 ELSE 0.0 END) AS family_score,
                AVG(CASE
                    WHEN lower(COALESCE(hb.market_segment, '')) IN ('corporate', 'offline ta/to', 'online ta')
                      OR lower(COALESCE(hb.customer_type, '')) IN ('transient', 'contract')
                    THEN 1.0 ELSE 0.0 END) AS business_score,
                AVG(COALESCE(hb.total_of_special_requests, 0)) AS avg_special_requests
            FROM hotel_bookings hb
            GROUP BY lower(hb.hotel)
        ) b ON b.hotel_key = lower(h.name)
        ORDER BY
            score DESC,
            h.rating DESC,
            h.review_count DESC
        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findHomeFeed(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT
            h.id,
            h.name,
            h.country,
            h.market_segment,
            h.photo_url,
            h.rating,
            h.review_count,
            h.created_at,
            COALESCE(b.avg_adr, 0),
            COALESCE(b.booking_count, 0),
            COALESCE(b.cancellation_rate, 0),
            COALESCE(b.repeated_guest_rate, 0),
            COALESCE(b.family_score, 0),
            COALESCE(b.business_score, 0),
            COALESCE(b.avg_special_requests, 0),
            (
                COALESCE(h.rating, 0) * 3
                + LN(GREATEST(COALESCE(h.review_count, 0), 1)) * 1.5
                + COALESCE(b.repeated_guest_rate, 0) * 2
                + COALESCE(b.family_score, 0)
                + COALESCE(b.business_score, 0)
                - COALESCE(b.cancellation_rate, 0) * 2.5
                - COALESCE(b.avg_adr, 0) / 250.0
            ) AS score
        FROM hotels h
        LEFT JOIN (
            SELECT
                lower(hb.hotel) AS hotel_key,
                AVG(COALESCE(hb.adr, 0)) AS avg_adr,
                COUNT(*)::int AS booking_count,
                AVG(CASE WHEN hb.is_canceled THEN 1.0 ELSE 0.0 END) AS cancellation_rate,
                AVG(CASE WHEN hb.is_repeated_guest THEN 1.0 ELSE 0.0 END) AS repeated_guest_rate,
                AVG(CASE WHEN COALESCE(hb.children, 0) + COALESCE(hb.babies, 0) > 0 THEN 1.0 ELSE 0.0 END) AS family_score,
                AVG(CASE
                    WHEN lower(COALESCE(hb.market_segment, '')) IN ('corporate', 'offline ta/to', 'online ta')
                      OR lower(COALESCE(hb.customer_type, '')) IN ('transient', 'contract')
                    THEN 1.0 ELSE 0.0 END) AS business_score,
                AVG(COALESCE(hb.total_of_special_requests, 0)) AS avg_special_requests
            FROM hotel_bookings hb
            GROUP BY lower(hb.hotel)
        ) b ON b.hotel_key = lower(h.name)
        WHERE h.created_at >= NOW() - INTERVAL '30 days'
        ORDER BY
            h.created_at DESC,
            score DESC
        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findNewHotels(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT
            h.id,
            h.name,
            h.country,
            h.market_segment,
            h.photo_url,
            h.rating,
            h.review_count,
            h.created_at,
            COALESCE(b.avg_adr, 0),
            COALESCE(b.booking_count, 0),
            COALESCE(b.cancellation_rate, 0),
            COALESCE(b.repeated_guest_rate, 0),
            COALESCE(b.family_score, 0),
            COALESCE(b.business_score, 0),
            COALESCE(b.avg_special_requests, 0),
            (
                COALESCE(h.rating, 0) * 3
                + LN(GREATEST(COALESCE(h.review_count, 0), 1)) * 1.5
                + COALESCE(b.repeated_guest_rate, 0) * 2
                + CASE WHEN :familyFriendly = TRUE THEN COALESCE(b.family_score, 0) * 3 ELSE COALESCE(b.family_score, 0) END
                + CASE WHEN :businessFriendly = TRUE THEN COALESCE(b.business_score, 0) * 3 ELSE COALESCE(b.business_score, 0) END
                - COALESCE(b.cancellation_rate, 0) * 2.5
                - COALESCE(b.avg_adr, 0) / 250.0
            ) AS score
        FROM hotels h
        LEFT JOIN (
            SELECT
                lower(hb.hotel) AS hotel_key,
                AVG(COALESCE(hb.adr, 0)) AS avg_adr,
                COUNT(*)::int AS booking_count,
                AVG(CASE WHEN hb.is_canceled THEN 1.0 ELSE 0.0 END) AS cancellation_rate,
                AVG(CASE WHEN hb.is_repeated_guest THEN 1.0 ELSE 0.0 END) AS repeated_guest_rate,
                AVG(CASE WHEN COALESCE(hb.children, 0) + COALESCE(hb.babies, 0) > 0 THEN 1.0 ELSE 0.0 END) AS family_score,
                AVG(CASE
                    WHEN lower(COALESCE(hb.market_segment, '')) IN ('corporate', 'offline ta/to', 'online ta')
                      OR lower(COALESCE(hb.customer_type, '')) IN ('transient', 'contract')
                    THEN 1.0 ELSE 0.0 END) AS business_score,
                AVG(COALESCE(hb.total_of_special_requests, 0)) AS avg_special_requests
            FROM hotel_bookings hb
            GROUP BY lower(hb.hotel)
        ) b ON b.hotel_key = lower(h.name)
        WHERE 1=1
          AND (:name IS NULL OR lower(h.name) LIKE CONCAT('%', lower(:name), '%'))
          AND (:country IS NULL OR lower(h.country) LIKE CONCAT('%', lower(:country), '%'))
          AND (:marketSegment IS NULL OR lower(h.market_segment) LIKE CONCAT('%', lower(:marketSegment), '%'))
          AND (:minRating IS NULL OR COALESCE(h.rating, 0) >= :minRating)
          AND (:maxAdr IS NULL OR COALESCE(b.avg_adr, 0) <= :maxAdr)
          AND (:customerType IS NULL OR EXISTS (
                SELECT 1
                FROM hotel_bookings hb2
                WHERE lower(hb2.hotel) = lower(h.name)
                  AND lower(COALESCE(hb2.customer_type, '')) = lower(:customerType)
          ))
          AND (:isRepeatedGuest IS NULL OR :isRepeatedGuest = FALSE OR COALESCE(b.repeated_guest_rate, 0) >= 0.15)
          AND (:familyFriendly IS NULL OR :familyFriendly = FALSE OR COALESCE(b.family_score, 0) >= 0.20)
          AND (:businessFriendly IS NULL OR :businessFriendly = FALSE OR COALESCE(b.business_score, 0) >= 0.20)
          AND (:lowCancellation IS NULL OR :lowCancellation = FALSE OR COALESCE(b.cancellation_rate, 0) <= 0.25)
        ORDER BY
          CASE WHEN :sortKey = 'new' THEN h.created_at END DESC,
          CASE WHEN :sortKey = 'rating' THEN h.rating END DESC,
          CASE WHEN :sortKey = 'review_count' THEN h.review_count END DESC,
          CASE WHEN :sortKey = 'adr_low' THEN COALESCE(b.avg_adr, 999999) END ASC,
          CASE WHEN :sortKey = 'adr_high' THEN COALESCE(b.avg_adr, 0) END DESC,
          CASE WHEN :sortKey = 'cancellation_low' THEN COALESCE(b.cancellation_rate, 1) END ASC,
          CASE WHEN :sortKey = 'family' THEN COALESCE(b.family_score, 0) END DESC,
          CASE WHEN :sortKey = 'business' THEN COALESCE(b.business_score, 0) END DESC,
          score DESC,
          h.rating DESC,
          h.review_count DESC
        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> searchHotels(
            @Param("name") String name,
            @Param("country") String country,
            @Param("marketSegment") String marketSegment,
            @Param("minRating") Double minRating,
            @Param("maxAdr") Double maxAdr,
            @Param("customerType") String customerType,
            @Param("isRepeatedGuest") Boolean isRepeatedGuest,
            @Param("familyFriendly") Boolean familyFriendly,
            @Param("businessFriendly") Boolean businessFriendly,
            @Param("lowCancellation") Boolean lowCancellation,
            @Param("sortKey") String sortKey,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
