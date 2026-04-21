package com.example.demo.repository;

import com.example.demo.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestoRepository extends JpaRepository<Restaurant, Long> {

    @Query(value = """
        SELECT
            r.id,
            r.name,
            r.location,
            r.cuisine,
            r.price,
            r.rating,
            r.review_count,
            r.photo_url,
            r.award,
            r.green_star,
            r.latitude,
            r.longitude,

            (
                COALESCE(r.rating, 0) * 3 +
                LOG(GREATEST(r.review_count, 1)) * 2
            ) AS score

        FROM restaurants r

        WHERE 1=1

        AND (:name IS NULL OR lower(r.name) LIKE CONCAT('%', lower(:name), '%'))
        AND (:location IS NULL OR lower(r.location) LIKE CONCAT('%', lower(:location), '%'))
        AND (:cuisine IS NULL OR lower(r.cuisine) LIKE CONCAT('%', lower(:cuisine), '%'))

        AND (:greenStar IS NULL OR r.green_star = :greenStar)
        AND (:award IS NULL OR r.award = :award)
        AND (:price IS NULL OR r.price = :price)

        AND (
            :lat IS NULL OR :lng IS NULL OR :radius IS NULL
            OR ST_DWithin(
                ST_SetSRID(ST_MakePoint(r.longitude, r.latitude), 4326)::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radius
            )
        )

        ORDER BY
            score DESC,
            r.rating DESC,
            r.review_count DESC

        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> searchRestaurants(
            @Param("name") String name,
            @Param("location") String location,
            @Param("cuisine") String cuisine,
            @Param("greenStar") Boolean greenStar,
            @Param("award") String award,
            @Param("price") String price,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT
            r.id,
            r.name,
            r.location,
            r.cuisine,
            r.price,
            r.rating,
            r.review_count,
            r.photo_url,
            r.award,
            r.green_star,
            r.latitude,
            r.longitude,

            (
                COALESCE(r.rating, 0) * 3 +
                LOG(GREATEST(r.review_count, 1)) * 2
            ) AS score

        FROM restaurants r

        ORDER BY
            score DESC,
            r.rating DESC,
            r.review_count DESC

        LIMIT :limit
        OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findHomeFeed(
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
