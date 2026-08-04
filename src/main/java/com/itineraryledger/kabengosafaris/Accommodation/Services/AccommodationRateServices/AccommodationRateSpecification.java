package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRateServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * AccommodationRateSpecification - Dynamic JPA Specifications for filtering AccommodationRate entities
 *
 * Provides reusable, composable specifications for building complex queries.
 *
 * AccommodationRate is uniquely identified by the combination of:
 * - Accommodation
 * - Season (accommodation-specific)
 * - AccommodationRoomType
 * - AccommodationRoomStandard
 * - AccommodationBoardType
 */
public class AccommodationRateSpecification {

    private AccommodationRateSpecification() {
        // Utility class - prevent instantiation
    }

    // ========================
    // ACCOMMODATION SPECIFICATIONS
    // ========================

    /**
     * Filter by accommodation ID
     */
    public static Specification<AccommodationRate> byAccommodationId(Long accommodationId) {
        return (root, query, cb) -> {
            if (accommodationId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("accommodation").get("id"), accommodationId);
        };
    }

    // ========================
    // SEASON SPECIFICATIONS
    // ========================

    /**
     * Filter by season ID
     */
    public static Specification<AccommodationRate> bySeasonId(Long seasonId) {
        return (root, query, cb) -> {
            if (seasonId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("season").get("id"), seasonId);
        };
    }

    // ========================
    // ROOM TYPE SPECIFICATIONS
    // ========================

    /**
     * Filter by room type ID
     */
    public static Specification<AccommodationRate> byRoomTypeId(Long roomTypeId) {
        return (root, query, cb) -> {
            if (roomTypeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("roomType").get("id"), roomTypeId);
        };
    }

    // ========================
    // ROOM STANDARD SPECIFICATIONS
    // ========================

    /**
     * Filter by room standard ID
     */
    public static Specification<AccommodationRate> byRoomStandardId(Long roomStandardId) {
        return (root, query, cb) -> {
            if (roomStandardId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("roomStandard").get("id"), roomStandardId);
        };
    }

    // ========================
    // BOARD TYPE SPECIFICATIONS
    // ========================

    /**
     * Filter by board type ID
     */
    public static Specification<AccommodationRate> byBoardTypeId(Long boardTypeId) {
        return (root, query, cb) -> {
            if (boardTypeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("boardType").get("id"), boardTypeId);
        };
    }

    // ========================
    // RATE SPECIFICATIONS
    // ========================

    /**
     * Filter by minimum rack rate
     */
    public static Specification<AccommodationRate> rackRateGreaterThanOrEqual(BigDecimal minRate) {
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
    public static Specification<AccommodationRate> rackRateLessThanOrEqual(BigDecimal maxRate) {
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
    public static Specification<AccommodationRate> hasStoRate() {
        return (root, query, cb) -> cb.isNotNull(root.get("stoRate"));
    }

    /**
     * Filter by currency
     */
    public static Specification<AccommodationRate> byCurrency(String currency) {
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
    public static Specification<AccommodationRate> isActive(Boolean isActive) {
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
    public static Specification<AccommodationRate> activeOnly() {
        return isActive(true);
    }

    /**
     * Filter by rate charging model (per person vs per room)
     *
     * @param isPerPerson true for Per Person Sharing (PPS), false for Per Room
     */
    public static Specification<AccommodationRate> isPerPerson(Boolean isPerPerson) {
        return (root, query, cb) -> {
            if (isPerPerson == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPerPerson"), isPerPerson);
        };
    }

    /**
     * Filter Per Person Sharing (PPS) rates only
     */
    public static Specification<AccommodationRate> perPersonOnly() {
        return isPerPerson(true);
    }

    /**
     * Filter Per Room rates only
     */
    public static Specification<AccommodationRate> perRoomOnly() {
        return isPerPerson(false);
    }

    // ========================
    // COMBINED SPECIFICATIONS
    // ========================

    /**
     * Full rate lookup by exact combination
     */
    public static Specification<AccommodationRate> forExactRate(
        Long accommodationId, Long seasonId, Long roomTypeId, Long roomStandardId, Long boardTypeId
    ) {
        return byAccommodationId(accommodationId)
            .and(bySeasonId(seasonId))
            .and(byRoomTypeId(roomTypeId))
            .and(byRoomStandardId(roomStandardId))
            .and(byBoardTypeId(boardTypeId));
    }

    /**
     * Rates for accommodation filtered by season
     */
    public static Specification<AccommodationRate> forAccommodationAndSeason(
        Long accommodationId, Long seasonId
    ) {
        return byAccommodationId(accommodationId)
            .and(bySeasonId(seasonId));
    }

    /** Recency, for the "new in the last N days" counters. */
    public static Specification<AccommodationRate> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }
}
