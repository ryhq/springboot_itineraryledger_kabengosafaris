package com.itineraryledger.kabengosafaris.ActivityTariffRate.Specifications;

import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * ActivityTariffRateSpecification - Dynamic JPA Specifications for filtering ActivityTariffRate entities
 *
 * Provides reusable, composable specifications for building complex queries.
 */
public class ActivityTariffRateSpecification {

    private ActivityTariffRateSpecification() {
        // Utility class - prevent instantiation
    }

    // ========================
    // ACTIVITY SPECIFICATIONS
    // ========================

    /**
     * Filter by activity ID
     */
    public static Specification<ActivityTariffRate> byActivityId(Long activityId) {
        return (root, query, cb) -> {
            if (activityId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("activity").get("id"), activityId);
        };
    }

    // ========================
    // PARK SPECIFICATIONS
    // ========================

    /**
     * Filter by park ID (null for global rates)
     */
    public static Specification<ActivityTariffRate> byParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.isNull(root.get("park"));
            }
            return cb.equal(root.get("park").get("id"), parkId);
        };
    }

    /**
     * Filter for global rates only (no park)
     */
    public static Specification<ActivityTariffRate> globalRatesOnly() {
        return (root, query, cb) -> cb.isNull(root.get("park"));
    }

    /**
     * Filter for park-specific rates only (has park)
     */
    public static Specification<ActivityTariffRate> parkSpecificRatesOnly() {
        return (root, query, cb) -> cb.isNotNull(root.get("park"));
    }

    // ========================
    // SEASON SPECIFICATIONS
    // ========================

    /**
     * Filter by season ID
     */
    public static Specification<ActivityTariffRate> bySeasonId(Long seasonId) {
        return (root, query, cb) -> {
            if (seasonId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("season").get("id"), seasonId);
        };
    }

    // ========================
    // NATION CATEGORY SPECIFICATIONS
    // ========================

    /**
     * Filter by nation category ID
     */
    public static Specification<ActivityTariffRate> byNationCategoryId(Long nationCategoryId) {
        return (root, query, cb) -> {
            if (nationCategoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("nationCategory").get("id"), nationCategoryId);
        };
    }

    // ========================
    // AGE CATEGORY SPECIFICATIONS
    // ========================

    /**
     * Filter by age category ID (null for non-PER_PERSON activities)
     */
    public static Specification<ActivityTariffRate> byAgeCategoryId(Long ageCategoryId) {
        return (root, query, cb) -> {
            if (ageCategoryId == null) {
                return cb.isNull(root.get("ageCategory"));
            }
            return cb.equal(root.get("ageCategory").get("id"), ageCategoryId);
        };
    }

    /**
     * Filter rates that have age category (PER_PERSON activities)
     */
    public static Specification<ActivityTariffRate> hasAgeCategory() {
        return (root, query, cb) -> cb.isNotNull(root.get("ageCategory"));
    }

    /**
     * Filter rates that don't have age category (vehicle/group activities)
     */
    public static Specification<ActivityTariffRate> noAgeCategory() {
        return (root, query, cb) -> cb.isNull(root.get("ageCategory"));
    }

    // ========================
    // RATE SPECIFICATIONS
    // ========================

    /**
     * Filter by minimum rack rate
     */
    public static Specification<ActivityTariffRate> rackRateGreaterThanOrEqual(BigDecimal minRate) {
        return (root, query, cb) -> {
            if (minRate == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("rackRate"), minRate);
        };
    }

    /**
     * Filter by maximum rack rate
     */
    public static Specification<ActivityTariffRate> rackRateLessThanOrEqual(BigDecimal maxRate) {
        return (root, query, cb) -> {
            if (maxRate == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("rackRate"), maxRate);
        };
    }

    /**
     * Filter rates that have STO rate
     */
    public static Specification<ActivityTariffRate> hasStoRate() {
        return (root, query, cb) -> cb.isNotNull(root.get("stoRate"));
    }

    /**
     * Filter by currency
     */
    public static Specification<ActivityTariffRate> byCurrency(String currency) {
        return (root, query, cb) -> {
            if (currency == null || currency.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("currency"), currency.trim().toUpperCase());
        };
    }

    // ========================
    // STATUS SPECIFICATIONS
    // ========================

    /**
     * Filter by active status
     */
    public static Specification<ActivityTariffRate> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter active rates only
     */
    public static Specification<ActivityTariffRate> activeOnly() {
        return isActive(true);
    }

    // ========================
    // COMBINED SPECIFICATIONS
    // ========================

    /**
     * Full rate lookup for PER_PERSON activity (with age category)
     */
    public static Specification<ActivityTariffRate> forPersonRate(
        Long activityId, Long parkId, Long seasonId, Long nationCategoryId, Long ageCategoryId
    ) {
        return byActivityId(activityId)
            .and(byParkId(parkId))
            .and(bySeasonId(seasonId))
            .and(byNationCategoryId(nationCategoryId))
            .and(byAgeCategoryId(ageCategoryId))
            .and(activeOnly());
    }

    /**
     * Full rate lookup for non-PER_PERSON activity (no age category)
     */
    public static Specification<ActivityTariffRate> forGroupRate(
        Long activityId, Long parkId, Long seasonId, Long nationCategoryId
    ) {
        return byActivityId(activityId)
            .and(byParkId(parkId))
            .and(bySeasonId(seasonId))
            .and(byNationCategoryId(nationCategoryId))
            .and(noAgeCategory())
            .and(activeOnly());
    }
}
