package com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.Season.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ActivityTariffRate entity
 *
 * Provides:
 * - Basic CRUD operations via JpaRepository
 * - Dynamic filtering via JpaSpecificationExecutor
 * - Custom query methods for rate lookups
 */
@Repository
public interface ActivityTariffRateRepository extends JpaRepository<ActivityTariffRate, Long>, JpaSpecificationExecutor<ActivityTariffRate> {

    // ========================
    // FIND BY ACTIVITY
    // ========================

    /**
     * Find all rates for an activity
     */
    List<ActivityTariffRate> findByActivityId(Long activityId);

    /**
     * Find all global rates for an activity (no park)
     */
    List<ActivityTariffRate> findByActivityIdAndParkIsNull(Long activityId);

    /**
     * Find all park-specific rates for an activity
     */
    List<ActivityTariffRate> findByActivityIdAndParkId(Long activityId, Long parkId);

    // ========================
    // FIND SPECIFIC RATES (PER_PERSON - with age category)
    // ========================

    /**
     * Find global rate for PER_PERSON activity (with age category, no park)
     */
    Optional<ActivityTariffRate> findByActivityIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIdAndParkIsNull(
        Long activityId, Long seasonId, Long nationCategoryId, Long ageCategoryId
    );

    /**
     * Find park-specific rate for PER_PERSON activity
     */
    Optional<ActivityTariffRate> findByActivityIdAndParkIdAndSeasonIdAndNationCategoryIdAndAgeCategoryId(
        Long activityId, Long parkId, Long seasonId, Long nationCategoryId, Long ageCategoryId
    );

    // ========================
    // FIND SPECIFIC RATES (non-PER_PERSON - no age category)
    // ========================

    /**
     * Find global rate for non-PER_PERSON activity (no age category, no park)
     */
    Optional<ActivityTariffRate> findByActivityIdAndSeasonIdAndNationCategoryIdAndParkIsNullAndAgeCategoryIsNull(
        Long activityId, Long seasonId, Long nationCategoryId
    );

    /**
     * Find park-specific rate for non-PER_PERSON activity
     */
    Optional<ActivityTariffRate> findByActivityIdAndParkIdAndSeasonIdAndNationCategoryIdAndAgeCategoryIsNull(
        Long activityId, Long parkId, Long seasonId, Long nationCategoryId
    );

    // ========================
    // ENTITY-BASED LOOKUPS (for service layer)
    // ========================

    /**
     * Find rate by activity, season, nation category, age category (global)
     */
    ActivityTariffRate findByActivityAndSeasonAndNationCategoryAndAgeCategoryAndParkIsNull(
        Activity activity, Season season, PaxNationCategory nationCategory, PaxAgeCategory ageCategory
    );

    /**
     * Find rate by activity, park, season, nation category, age category
     */
    ActivityTariffRate findByActivityAndParkAndSeasonAndNationCategoryAndAgeCategory(
        Activity activity, Park park, Season season, PaxNationCategory nationCategory, PaxAgeCategory ageCategory
    );

    /**
     * Find rate for non-PER_PERSON activity (no age category, global)
     */
    ActivityTariffRate findByActivityAndSeasonAndNationCategoryAndAgeCategoryIsNullAndParkIsNull(
        Activity activity, Season season, PaxNationCategory nationCategory
    );

    /**
     * Find rate for non-PER_PERSON activity (no age category, park-specific)
     */
    ActivityTariffRate findByActivityAndParkAndSeasonAndNationCategoryAndAgeCategoryIsNull(
        Activity activity, Park park, Season season, PaxNationCategory nationCategory
    );

    // ========================
    // BULK LOOKUPS
    // ========================

    /**
     * Find rates for activity across multiple seasons and nation categories (with age categories)
     */
    List<ActivityTariffRate> findByActivityAndSeasonInAndNationCategoryInAndAgeCategoryIn(
        Activity activity, List<Season> seasons, List<PaxNationCategory> nationCategories, List<PaxAgeCategory> ageCategories
    );

    /**
     * Find rates for activity across multiple seasons and nation categories (no age category)
     */
    List<ActivityTariffRate> findByActivityAndSeasonInAndNationCategoryInAndAgeCategoryIsNull(
        Activity activity, List<Season> seasons, List<PaxNationCategory> nationCategories
    );

    /**
     * Find park-specific rates for activity across multiple seasons and nation categories (with age categories)
     */
    List<ActivityTariffRate> findByParkAndActivityAndSeasonInAndNationCategoryInAndAgeCategoryIn(
        Park park, Activity activity, List<Season> seasons, List<PaxNationCategory> nationCategories, List<PaxAgeCategory> ageCategories
    );

    // ========================
    // ACTIVE RATE LOOKUPS (for cost estimation)
    // ========================

    /**
     * Find ACTIVE global rate for PER_PERSON activity (with age category, no park)
     */
    @Query("SELECT r FROM ActivityTariffRate r WHERE " +
           "r.activity.id = :activityId AND " +
           "r.season.id = :seasonId AND " +
           "r.nationCategory.id = :nationCategoryId AND " +
           "r.ageCategory.id = :ageCategoryId AND " +
           "r.park IS NULL AND " +
           "r.isActive = true")
    Optional<ActivityTariffRate> findActiveGlobalRateForPerson(
        @Param("activityId") Long activityId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Find ACTIVE park-specific rate for PER_PERSON activity
     */
    @Query("SELECT r FROM ActivityTariffRate r WHERE " +
           "r.activity.id = :activityId AND " +
           "r.park.id = :parkId AND " +
           "r.season.id = :seasonId AND " +
           "r.nationCategory.id = :nationCategoryId AND " +
           "r.ageCategory.id = :ageCategoryId AND " +
           "r.isActive = true")
    Optional<ActivityTariffRate> findActiveParkRateForPerson(
        @Param("activityId") Long activityId,
        @Param("parkId") Long parkId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Find ACTIVE global rate for non-PER_PERSON activity (no age category, no park)
     */
    @Query("SELECT r FROM ActivityTariffRate r WHERE " +
           "r.activity.id = :activityId AND " +
           "r.season.id = :seasonId AND " +
           "r.nationCategory.id = :nationCategoryId AND " +
           "r.park IS NULL AND " +
           "r.ageCategory IS NULL AND " +
           "r.isActive = true")
    Optional<ActivityTariffRate> findActiveGlobalRateForGroup(
        @Param("activityId") Long activityId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId
    );

    /**
     * Find ACTIVE park-specific rate for non-PER_PERSON activity
     */
    @Query("SELECT r FROM ActivityTariffRate r WHERE " +
           "r.activity.id = :activityId AND " +
           "r.park.id = :parkId AND " +
           "r.season.id = :seasonId AND " +
           "r.nationCategory.id = :nationCategoryId AND " +
           "r.ageCategory IS NULL AND " +
           "r.isActive = true")
    Optional<ActivityTariffRate> findActiveParkRateForGroup(
        @Param("activityId") Long activityId,
        @Param("parkId") Long parkId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId
    );

    // ========================
    // EXISTENCE CHECKS
    // ========================

    /**
     * Check if rate exists for exact combination (with park and age category)
     */
    @Query("SELECT COUNT(r) > 0 FROM ActivityTariffRate r WHERE " +
           "r.activity.id = :activityId AND " +
           "(:parkId IS NULL AND r.park IS NULL OR r.park.id = :parkId) AND " +
           "r.season.id = :seasonId AND " +
           "r.nationCategory.id = :nationCategoryId AND " +
           "(:ageCategoryId IS NULL AND r.ageCategory IS NULL OR r.ageCategory.id = :ageCategoryId)")
    boolean existsByExactCombination(
        @Param("activityId") Long activityId,
        @Param("parkId") Long parkId,
        @Param("seasonId") Long seasonId,
        @Param("nationCategoryId") Long nationCategoryId,
        @Param("ageCategoryId") Long ageCategoryId
    );

    /**
     * Check if any rates exist for an activity
     */
    boolean existsByActivityId(Long activityId);

    // ========================
    // COUNT METHODS
    // ========================

    /**
     * Count rates for an activity
     */
    long countByActivityId(Long activityId);

    /**
     * Count global rates for an activity
     */
    long countByActivityIdAndParkIsNull(Long activityId);

    /**
     * Count park-specific rates for an activity
     */
    long countByActivityIdAndParkId(Long activityId, Long parkId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT r.id FROM ActivityTariffRate r WHERE r.id > :currentId ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM ActivityTariffRate r WHERE r.id < :currentId ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM ActivityTariffRate r ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT r.id FROM ActivityTariffRate r ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // DELETE METHODS
    // ========================

    /**
     * Delete all rates for an activity
     */
    void deleteByActivityId(Long activityId);

    /**
     * Delete all park-specific rates for an activity
     */
    void deleteByActivityIdAndParkId(Long activityId, Long parkId);
}
