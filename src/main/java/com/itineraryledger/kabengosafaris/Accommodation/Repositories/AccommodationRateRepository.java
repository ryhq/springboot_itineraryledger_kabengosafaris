package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Season.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AccommodationRate entity
 *
 * Provides:
 * - Basic CRUD operations via JpaRepository
 * - Dynamic filtering via JpaSpecificationExecutor
 * - Custom query methods for rate lookups
 *
 * AccommodationRate is uniquely identified by the combination of:
 * - Accommodation
 * - Season (accommodation-specific)
 * - AccommodationRoomType
 * - AccommodationRoomStandard
 * - AccommodationBoardType
 */
@Repository
public interface AccommodationRateRepository extends JpaRepository<AccommodationRate, Long>, JpaSpecificationExecutor<AccommodationRate> {

    // ========================
    // FIND BY ACCOMMODATION
    // ========================

    /**
     * Find all rates for an accommodation
     */
    List<AccommodationRate> findByAccommodationId(Long accommodationId);

    /**
     * Find rates for an accommodation and season
     */
    List<AccommodationRate> findByAccommodationIdAndSeasonId(Long accommodationId, Long seasonId);

    // ========================
    // FIND SPECIFIC RATES
    // ========================

    /**
     * Find rate by exact combination
     */
    Optional<AccommodationRate> findByAccommodationIdAndSeasonIdAndRoomTypeIdAndRoomStandardIdAndBoardTypeId(
        Long accommodationId, Long seasonId, Long roomTypeId, Long roomStandardId, Long boardTypeId
    );

    /**
     * Find rate by entities
     */
    Optional<AccommodationRate> findByAccommodationAndSeasonAndRoomTypeAndRoomStandardAndBoardType(
        Accommodation accommodation, Season season, AccommodationRoomType roomType,
        AccommodationRoomStandard roomStandard, AccommodationBoardType boardType
    );

    // ========================
    // BULK LOOKUPS
    // ========================

    /**
     * Find rates for accommodation with multiple seasons
     */
    List<AccommodationRate> findByAccommodationIdAndSeasonIdIn(Long accommodationId, List<Long> seasonIds);

    /**
     * Find rates for accommodation with multiple room types
     */
    List<AccommodationRate> findByAccommodationIdAndRoomTypeIdIn(Long accommodationId, List<Long> roomTypeIds);

    /**
     * Find rates for accommodation with multiple room standards
     */
    List<AccommodationRate> findByAccommodationIdAndRoomStandardIdIn(Long accommodationId, List<Long> roomStandardIds);

    /**
     * Find rates for accommodation with multiple board types
     */
    List<AccommodationRate> findByAccommodationIdAndBoardTypeIdIn(Long accommodationId, List<Long> boardTypeIds);

    // ========================
    // ACTIVE RATE LOOKUPS (for cost estimation)
    // ========================

    /**
     * Find ACTIVE rate by exact combination
     */
    @Query("SELECT r FROM AccommodationRate r WHERE " +
           "r.accommodation.id = :accommodationId AND " +
           "r.season.id = :seasonId AND " +
           "r.roomType.id = :roomTypeId AND " +
           "r.roomStandard.id = :roomStandardId AND " +
           "r.boardType.id = :boardTypeId AND " +
           "r.isActive = true")
    Optional<AccommodationRate> findActiveRate(
        @Param("accommodationId") Long accommodationId,
        @Param("seasonId") Long seasonId,
        @Param("roomTypeId") Long roomTypeId,
        @Param("roomStandardId") Long roomStandardId,
        @Param("boardTypeId") Long boardTypeId
    );

    /**
     * Find all ACTIVE rates for an accommodation
     */
    List<AccommodationRate> findByAccommodationIdAndIsActiveTrue(Long accommodationId);

    /**
     * Find all ACTIVE rates for an accommodation and season
     */
    List<AccommodationRate> findByAccommodationIdAndSeasonIdAndIsActiveTrue(Long accommodationId, Long seasonId);

    // ========================
    // EXISTENCE CHECKS
    // ========================

    /**
     * Check if rate exists for exact combination
     */
    @Query("SELECT COUNT(r) > 0 FROM AccommodationRate r WHERE " +
           "r.accommodation.id = :accommodationId AND " +
           "r.season.id = :seasonId AND " +
           "r.roomType.id = :roomTypeId AND " +
           "r.roomStandard.id = :roomStandardId AND " +
           "r.boardType.id = :boardTypeId")
    boolean existsByExactCombination(
        @Param("accommodationId") Long accommodationId,
        @Param("seasonId") Long seasonId,
        @Param("roomTypeId") Long roomTypeId,
        @Param("roomStandardId") Long roomStandardId,
        @Param("boardTypeId") Long boardTypeId
    );

    /**
     * Check if any rates exist for an accommodation
     */
    boolean existsByAccommodationId(Long accommodationId);

    /**
     * Check if any rates exist for a season
     */
    boolean existsBySeasonId(Long seasonId);

    /**
     * Check if any rates exist for a room type
     */
    boolean existsByRoomTypeId(Long roomTypeId);

    /**
     * Check if any rates exist for a room standard
     */
    boolean existsByRoomStandardId(Long roomStandardId);

    /**
     * Check if any rates exist for a board type
     */
    boolean existsByBoardTypeId(Long boardTypeId);

    // ========================
    // COUNT METHODS
    // ========================

    /**
     * Count rates for an accommodation
     */
    long countByAccommodationId(Long accommodationId);

    /**
     * Count rates for a season
     */
    long countBySeasonId(Long seasonId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT r.id FROM AccommodationRate r WHERE r.id > :currentId ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM AccommodationRate r WHERE r.id < :currentId ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM AccommodationRate r ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT r.id FROM AccommodationRate r ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // DELETE METHODS
    // ========================

    /**
     * Delete all rates for an accommodation
     */
    void deleteByAccommodationId(Long accommodationId);

    /**
     * Delete all rates for a season
     */
    void deleteBySeasonId(Long seasonId);

    /**
     * Delete all rates for a room type
     */
    void deleteByRoomTypeId(Long roomTypeId);

    /**
     * Delete all rates for a room standard
     */
    void deleteByRoomStandardId(Long roomStandardId);

    /**
     * Delete all rates for a board type
     */
    void deleteByBoardTypeId(Long boardTypeId);
}
