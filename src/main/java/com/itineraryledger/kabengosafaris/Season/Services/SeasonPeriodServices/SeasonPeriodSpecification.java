package com.itineraryledger.kabengosafaris.Season.Services.SeasonPeriodServices;

import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import org.springframework.data.jpa.domain.Specification;

import java.time.MonthDay;

/**
 * SeasonPeriodSpecification - Provides reusable Specification objects for filtering SeasonPeriod entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<SeasonPeriod> that can be combined with other specifications
 */
public class SeasonPeriodSpecification {

    /**
     * Filter by season ID
     */
    public static Specification<SeasonPeriod> hasSeasonId(Long seasonId) {
        return (root, query, cb) -> {
            if (seasonId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("season").get("id"), seasonId);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<SeasonPeriod> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by system status
     * TRUE = System periods (protected from deletion, created by initializer)
     * FALSE = User-created periods (can be deleted)
     */
    public static Specification<SeasonPeriod> isSystem(Boolean isSystem) {
        return (root, query, cb) -> {
            if (isSystem == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isSystem"), isSystem);
        };
    }

    /**
     * Filter by specific year
     */
    public static Specification<SeasonPeriod> hasYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("year"), year);
        };
    }

    /**
     * Filter recurring periods (year is null)
     */
    public static Specification<SeasonPeriod> isRecurring() {
        return (root, query, cb) -> cb.isNull(root.get("year"));
    }

    /**
     * Filter non-recurring periods (year is not null)
     */
    public static Specification<SeasonPeriod> isNonRecurring() {
        return (root, query, cb) -> cb.isNotNull(root.get("year"));
    }

    /**
     * Filter by start date
     */
    public static Specification<SeasonPeriod> hasStartDate(MonthDay startDate) {
        return (root, query, cb) -> {
            if (startDate == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("startDate"), startDate);
        };
    }

    /**
     * Filter by end date
     */
    public static Specification<SeasonPeriod> hasEndDate(MonthDay endDate) {
        return (root, query, cb) -> {
            if (endDate == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("endDate"), endDate);
        };
    }

    /**
     * Filter by notes (case-insensitive partial match)
     */
    public static Specification<SeasonPeriod> notesLike(String notes) {
        return (root, query, cb) -> {
            if (notes == null || notes.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("notes")), "%" + notes.toLowerCase() + "%");
        };
    }

    /**
     * Filter system periods only (protected from deletion)
     * Returns periods created by the initializer
     */
    public static Specification<SeasonPeriod> systemPeriodsOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("isSystem"));
    }

    /**
     * Filter non-system periods only (can be deleted)
     */
    public static Specification<SeasonPeriod> nonSystemPeriodsOnly() {
        return (root, query, cb) -> cb.isFalse(root.get("isSystem"));
    }

    /**
     * Filter active system periods
     */
    public static Specification<SeasonPeriod> activeSystemPeriods() {
        return (root, query, cb) -> cb.and(
            cb.isTrue(root.get("isSystem")),
            cb.isTrue(root.get("isActive"))
        );
    }

    /**
     * Filter active periods for a specific season
     */
    public static Specification<SeasonPeriod> activePeriodsForSeason(Long seasonId) {
        return (root, query, cb) -> {
            if (seasonId == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.equal(root.get("season").get("id"), seasonId),
                cb.isTrue(root.get("isActive"))
            );
        };
    }

    /**
     * Filter periods by year range
     */
    public static Specification<SeasonPeriod> yearBetween(Integer minYear, Integer maxYear) {
        return (root, query, cb) -> {
            if (minYear == null && maxYear == null) {
                return cb.conjunction();
            }
            if (minYear != null && maxYear != null) {
                return cb.between(root.get("year"), minYear, maxYear);
            } else if (minYear != null) {
                return cb.greaterThanOrEqualTo(root.get("year"), minYear);
            } else {
                return cb.lessThanOrEqualTo(root.get("year"), maxYear);
            }
        };
    }
}
