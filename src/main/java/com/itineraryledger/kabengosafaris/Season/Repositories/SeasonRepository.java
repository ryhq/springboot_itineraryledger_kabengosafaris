package com.itineraryledger.kabengosafaris.Season.Repositories;

import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Season.SeasonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * The one global season with this name, for matching a season across installations.
     *
     * A season carries no slug and its name is unique only within its scope, so the scope is part of
     * the lookup: a company-wide "High Season" is a different row from a lodge's own "High Season",
     * and an import that conflated them would attach a lodge's rates to somebody else's dates.
     */
    Optional<Season> findByIsGlobalTrueAndNameIgnoreCase(String name);

    /** The same question for a season belonging to one accommodation. */
    Optional<Season> findByAccommodationIdAndNameIgnoreCase(Long accommodationId, String name);

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

    /**
     * Find all active seasons for an accommodation
     */
    List<Season> findByAccommodationIdAndIsActiveTrue(Long accommodationId);

    /** Every season belonging to one lodge, active or not — an export carries what is there. */
    List<Season> findByAccommodationId(Long accommodationId);

    /**
     * Check if an accommodation has any seasons configured
     */
    boolean existsByAccommodationId(Long accommodationId);

    /**
     * Find active season for an accommodation by season type
     */
    @Query("""
        SELECT s FROM Season s
        WHERE s.accommodation.id = :accommodationId
        AND s.seasonType = :seasonType
        AND s.isActive = true
        """)
    Optional<Season> findActiveByAccommodationIdAndSeasonType(
        @Param("accommodationId") Long accommodationId,
        @Param("seasonType") SeasonType seasonType
    );

    /**
     * Find all active seasons for an accommodation ordered by season type priority.
     * Priority order: HIGH_SEASON > PEAK_SEASON > SHOULDER_SEASON > FESTIVE_SEASON > SPECIAL_EVENT > LOW_SEASON > STANDARD > CUSTOM
     */
    @Query("""
        SELECT s FROM Season s
        WHERE s.accommodation.id = :accommodationId
        AND s.isActive = true
        ORDER BY
            CASE s.seasonType
                WHEN 'HIGH_SEASON' THEN 1
                WHEN 'PEAK_SEASON' THEN 2
                WHEN 'SHOULDER_SEASON' THEN 3
                WHEN 'FESTIVE_SEASON' THEN 4
                WHEN 'SPECIAL_EVENT' THEN 5
                WHEN 'LOW_SEASON' THEN 6
                WHEN 'STANDARD' THEN 7
                WHEN 'CUSTOM' THEN 8
                ELSE 9
            END ASC
        """)
    List<Season> findActiveByAccommodationIdOrderedByTypePriority(@Param("accommodationId") Long accommodationId);

    @Query("SELECT e.id FROM Season e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Season e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Season e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Season e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
