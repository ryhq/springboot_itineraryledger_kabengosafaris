package com.itineraryledger.kabengosafaris.Season.Repositories;

import com.itineraryledger.kabengosafaris.Season.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SeasonRepository - Data access layer for Season entity
 */
@Repository
public interface SeasonRepository extends JpaRepository<Season, Long>, JpaSpecificationExecutor<Season> {

    /**
     * Check if a season name already exists for an accommodation
     */
    boolean existsByAccommodationIdAndName(Long accommodationId, String name);

    /**
     * Check if a global season name already exists
     */
    boolean existsByIsGlobalTrueAndName(String name);

    /**
     * Count global seasons
     */
    long countByIsGlobalTrue();

    /**
     * Count accommodation-specific seasons
     */
    long countByAccommodationId(Long accommodationId);

    /**
     * Find unique seasons based on season type
     * Returns one season per unique season type, sorted by name
     * Useful for dropdowns where users select existing season type configurations
     */
    @Query("""
        SELECT s FROM Season s
        WHERE s.id IN (
            SELECT MIN(s2.id)
            FROM Season s2
            WHERE s2.isActive = true
            GROUP BY s2.seasonType
        )
        ORDER BY s.name ASC
        """)
    List<Season> findUniqueSeasonsByType();
}
