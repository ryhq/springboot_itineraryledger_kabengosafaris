package com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories;

import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkTariffRate entity
 *
 * Provides:
 * - Basic CRUD operations via JpaRepository
 * - Dynamic filtering via JpaSpecificationExecutor
 * - Custom query methods for rate lookups
 */
@Repository
public interface ParkTariffRateRepository extends JpaRepository<ParkTariffRate, Long>, JpaSpecificationExecutor<ParkTariffRate> {

    // ========================
    // FIND BY PARK-TARIFF
    // ========================

    /**
     * Find all rates for a specific park-tariff combination
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId AND ptr.parkTariff.tariff.id = :tariffId")
    List<ParkTariffRate> findByParkIdAndTariffId(@Param("parkId") Long parkId, @Param("tariffId") Long tariffId);

    /**
     * Find all rates for a specific park
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId")
    List<ParkTariffRate> findByParkId(@Param("parkId") Long parkId);

    /**
     * Find all rates for a specific tariff (across all parks)
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE ptr.parkTariff.tariff.id = :tariffId")
    List<ParkTariffRate> findByTariffId(@Param("tariffId") Long tariffId);

    // ========================
    // FIND SPECIFIC RATE
    // ========================

    /**
     * Find exact rate by all criteria (full match)
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE " +
           "ptr.parkTariff.park.id = :parkId AND " +
           "ptr.parkTariff.tariff.id = :tariffId AND " +
           "ptr.season.id = :seasonId AND " +
           "ptr.nationCategory.id = :nationCategoryId AND " +
           "(:ageCategoryId IS NULL AND ptr.ageCategory IS NULL OR ptr.ageCategory.id = :ageCategoryId)")
    Optional<ParkTariffRate> findExactRate(
        @Param("parkId") Long parkId,
        @Param("tariffId") Long tariffId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Find rate for PER_PERSON tariff (with age category)
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE " +
           "ptr.parkTariff.park.id = :parkId AND " +
           "ptr.parkTariff.tariff.id = :tariffId AND " +
           "ptr.season.id = :seasonId AND " +
           "ptr.nationCategory.id = :nationCategoryId AND " +
           "ptr.ageCategory.id = :ageCategoryId AND " +
           "ptr.isActive = true")
    Optional<ParkTariffRate> findActiveRateForPerson(
        @Param("parkId") Long parkId,
        @Param("tariffId") Long tariffId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Find rate for non-PER_PERSON tariff (vehicle/group/flat - no age category)
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE " +
           "ptr.parkTariff.park.id = :parkId AND " +
           "ptr.parkTariff.tariff.id = :tariffId AND " +
           "ptr.season.id = :seasonId AND " +
           "ptr.nationCategory.id = :nationCategoryId AND " +
           "ptr.ageCategory IS NULL AND " +
           "ptr.isActive = true")
    Optional<ParkTariffRate> findActiveRateForGroup(
        @Param("parkId") Long parkId,
        @Param("tariffId") Long tariffId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId
    );

    // ========================
    // FIND BY SEASON
    // ========================

    /**
     * Find all rates for a specific season
     */
    List<ParkTariffRate> findBySeasonId(Long seasonId);

    /**
     * Find rates for park and season
     */
    @Query("SELECT ptr FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId AND ptr.season.id = :seasonId")
    List<ParkTariffRate> findByParkIdAndSeasonId(@Param("parkId") Long parkId, @Param("seasonId") Long seasonId);

    // ========================
    // FIND BY NATION CATEGORY
    // ========================

    /**
     * Find all rates for a specific nation category
     */
    List<ParkTariffRate> findByNationCategoryId(Long nationCategoryId);

    // ========================
    // FIND BY AGE CATEGORY
    // ========================

    /**
     * Find all rates for a specific age category
     */
    List<ParkTariffRate> findByAgeCategoryId(Long ageCategoryId);

    // ========================
    // EXISTENCE CHECKS
    // ========================

    /**
     * Check if rate exists for exact combination
     */
    @Query("SELECT COUNT(ptr) > 0 FROM ParkTariffRate ptr WHERE " +
           "ptr.parkTariff.park.id = :parkId AND " +
           "ptr.parkTariff.tariff.id = :tariffId AND " +
           "ptr.season.id = :seasonId AND " +
           "ptr.nationCategory.id = :nationCategoryId AND " +
           "(:ageCategoryId IS NULL AND ptr.ageCategory IS NULL OR ptr.ageCategory.id = :ageCategoryId)")
    boolean existsByExactCombination(
        @Param("parkId") Long parkId,
        @Param("tariffId") Long tariffId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Check if any rates exist for a park-tariff combination
     */
    @Query("SELECT COUNT(ptr) > 0 FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId AND ptr.parkTariff.tariff.id = :tariffId")
    boolean existsByParkIdAndTariffId(@Param("parkId") Long parkId, @Param("tariffId") Long tariffId);

    // ========================
    // COUNT METHODS
    // ========================

    /**
     * Count rates for a park
     */
    @Query("SELECT COUNT(ptr) FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId")
    long countByParkId(@Param("parkId") Long parkId);

    /**
     * Count rates for a tariff
     */
    @Query("SELECT COUNT(ptr) FROM ParkTariffRate ptr WHERE ptr.parkTariff.tariff.id = :tariffId")
    long countByTariffId(@Param("tariffId") Long tariffId);

    /**
     * Count active rates for a park-tariff combination
     */
    @Query("SELECT COUNT(ptr) FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId AND ptr.parkTariff.tariff.id = :tariffId AND ptr.isActive = true")
    long countActiveByParkIdAndTariffId(@Param("parkId") Long parkId, @Param("tariffId") Long tariffId);

    // ========================
    // DELETE METHODS
    // ========================

    /**
     * Delete all rates for a park-tariff combination
     */
    @Query("DELETE FROM ParkTariffRate ptr WHERE ptr.parkTariff.park.id = :parkId AND ptr.parkTariff.tariff.id = :tariffId")
    void deleteByParkIdAndTariffId(@Param("parkId") Long parkId, @Param("tariffId") Long tariffId);
}
