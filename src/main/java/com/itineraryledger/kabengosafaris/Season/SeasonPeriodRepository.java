package com.itineraryledger.kabengosafaris.Season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonPeriodRepository extends JpaRepository<SeasonPeriod, Long> {

    /**
     * Find all periods for a specific season
     */
    List<SeasonPeriod> findBySeason(Season season);

    /**
     * Find periods by season ID
     */
    List<SeasonPeriod> findBySeasonId(Long seasonId);

    /**
     * Find periods for a specific year
     */
    List<SeasonPeriod> findByYear(Integer year);

    /**
     * Find recurring periods (no specific year)
     */
    List<SeasonPeriod> findByYearIsNull();

    /**
     * Find periods by season and year
     */
    List<SeasonPeriod> findBySeasonAndYear(Season season, Integer year);

    /**
     * Find recurring periods for a season
     */
    List<SeasonPeriod> findBySeasonAndYearIsNull(Season season);

    /**
     * Find all periods ordered by start date
     */
    @Query("SELECT sp FROM SeasonPeriod sp WHERE sp.season = :season ORDER BY sp.year ASC NULLS LAST, sp.startDate ASC")
    List<SeasonPeriod> findBySeasonOrderedByStartDate(@Param("season") Season season);

    /**
     * Check for overlapping periods within the same season
     * Note: This is a simplified check. Complex overlap detection (especially with year-wrapping)
     * is better handled in service layer
     */
    @Query("SELECT sp FROM SeasonPeriod sp WHERE sp.season = :season " +
           "AND sp.id != :excludeId " +
           "AND ((sp.year = :year) OR (sp.year IS NULL AND :year IS NULL))")
    List<SeasonPeriod> findPotentialOverlappingPeriods(
        @Param("season") Season season,
        @Param("year") Integer year,
        @Param("excludeId") Long excludeId
    );

    /**
     * Count periods for a season
     */
    long countBySeason(Season season);

    /**
     * Count recurring periods for a season
     */
    long countBySeasonAndYearIsNull(Season season);

    /**
     * Delete all periods for a season
     */
    void deleteBySeason(Season season);
}
