package com.itineraryledger.kabengosafaris.ParkTariffRate.Specifications;

import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * ParkTariffRateSpecification - Dynamic JPA Specifications for filtering ParkTariffRate entities
 *
 * Provides reusable, composable specifications for building complex queries.
 */
public class ParkTariffRateSpecification {

    private ParkTariffRateSpecification() {
        // Utility class - prevent instantiation
    }

    // ========================
    // PARK-TARIFF SPECIFICATIONS
    // ========================

    /**
     * Filter by park ID
     */
    public static Specification<ParkTariffRate> byParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("parkTariff").get("park").get("id"), parkId);
        };
    }

    /**
     * Filter by tariff ID
     */
    public static Specification<ParkTariffRate> byTariffId(Long tariffId) {
        return (root, query, cb) -> {
            if (tariffId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("parkTariff").get("tariff").get("id"), tariffId);
        };
    }

    /**
     * Filter by park and tariff
     */
    public static Specification<ParkTariffRate> byParkAndTariff(Long parkId, Long tariffId) {
        return byParkId(parkId).and(byTariffId(tariffId));
    }

    // ========================
    // SEASON SPECIFICATIONS
    // ========================

    /**
     * Filter by season ID
     */
    public static Specification<ParkTariffRate> bySeasonId(Long seasonId) {
        return (root, query, cb) -> {
            if (seasonId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("season").get("id"), seasonId);
        };
    }

    /**
     * Filter by global seasons only
     */
    public static Specification<ParkTariffRate> byGlobalSeason() {
        return (root, query, cb) -> cb.equal(root.get("season").get("isGlobal"), true);
    }

    // ========================
    // NATION CATEGORY SPECIFICATIONS
    // ========================

    /**
     * Filter by nation category ID
     */
    public static Specification<ParkTariffRate> byNationCategoryId(Long nationCategoryId) {
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
     * Filter by age category ID (null for non-PER_PERSON tariffs)
     */
    public static Specification<ParkTariffRate> byAgeCategoryId(Long ageCategoryId) {
        return (root, query, cb) -> {
            if (ageCategoryId == null) {
                return cb.isNull(root.get("ageCategory"));
            }
            return cb.equal(root.get("ageCategory").get("id"), ageCategoryId);
        };
    }

    /**
     * Filter rates that have age category (PER_PERSON tariffs)
     */
    public static Specification<ParkTariffRate> hasAgeCategory() {
        return (root, query, cb) -> cb.isNotNull(root.get("ageCategory"));
    }

    /**
     * Filter rates that don't have age category (vehicle/group/flat tariffs)
     */
    public static Specification<ParkTariffRate> noAgeCategory() {
        return (root, query, cb) -> cb.isNull(root.get("ageCategory"));
    }

    // ========================
    // RATE SPECIFICATIONS
    // ========================

    /**
     * Filter by minimum rack rate
     */
    public static Specification<ParkTariffRate> rackRateGreaterThanOrEqual(BigDecimal minRate) {
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
    public static Specification<ParkTariffRate> rackRateLessThanOrEqual(BigDecimal maxRate) {
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
    public static Specification<ParkTariffRate> hasStoRate() {
        return (root, query, cb) -> cb.isNotNull(root.get("stoRate"));
    }

    /**
     * Filter by currency
     */
    public static Specification<ParkTariffRate> byCurrency(String currency) {
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
    public static Specification<ParkTariffRate> isActive(Boolean isActive) {
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
    public static Specification<ParkTariffRate> activeOnly() {
        return isActive(true);
    }

    // ========================
    // COMBINED SPECIFICATIONS
    // ========================

    /**
     * Full rate lookup specification (for PER_PERSON tariffs)
     */
    public static Specification<ParkTariffRate> forPersonRate(
        Long parkId, Long tariffId, Long seasonId, Long nationCategoryId, Long ageCategoryId
    ) {
        return byParkAndTariff(parkId, tariffId)
            .and(bySeasonId(seasonId))
            .and(byNationCategoryId(nationCategoryId))
            .and(byAgeCategoryId(ageCategoryId))
            .and(activeOnly());
    }

    /**
     * Full rate lookup specification (for non-PER_PERSON tariffs)
     */
    public static Specification<ParkTariffRate> forGroupRate(
        Long parkId, Long tariffId, Long seasonId, Long nationCategoryId
    ) {
        return byParkAndTariff(parkId, tariffId)
            .and(bySeasonId(seasonId))
            .and(byNationCategoryId(nationCategoryId))
            .and(noAgeCategory())
            .and(activeOnly());
    }
}
