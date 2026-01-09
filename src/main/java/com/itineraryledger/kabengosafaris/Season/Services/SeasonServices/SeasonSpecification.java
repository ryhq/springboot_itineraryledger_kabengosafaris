package com.itineraryledger.kabengosafaris.Season.Services.SeasonServices;

import com.itineraryledger.kabengosafaris.Season.Season;
import org.springframework.data.jpa.domain.Specification;

/**
 * SeasonSpecification - Provides reusable Specification objects for filtering Season entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Season> that can be combined with other specifications
 */
public class SeasonSpecification {

    /**
     * Filter by season name (case-insensitive partial match)
     */
    public static Specification<Season> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by exact season name (case-insensitive)
     */
    public static Specification<Season> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("name")), name.toLowerCase());
        };
    }

    /**
     * Filter by season type
     */
    public static Specification<Season> hasSeasonType(Season.SeasonType seasonType) {
        return (root, query, cb) -> {
            if (seasonType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("seasonType"), seasonType);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Season> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by global status
     * TRUE = Global seasons (not tied to any accommodation)
     * FALSE = Accommodation-specific seasons
     */
    public static Specification<Season> isGlobal(Boolean isGlobal) {
        return (root, query, cb) -> {
            if (isGlobal == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isGlobal"), isGlobal);
        };
    }

    /**
     * Filter by system status
     * TRUE = System seasons (protected from deletion, created by initializer)
     * FALSE = User-created seasons (can be deleted)
     */
    public static Specification<Season> isSystem(Boolean isSystem) {
        return (root, query, cb) -> {
            if (isSystem == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isSystem"), isSystem);
        };
    }

    /**
     * Filter by accommodation ID
     * Returns only seasons for a specific accommodation
     */
    public static Specification<Season> hasAccommodationId(Long accommodationId) {
        return (root, query, cb) -> {
            if (accommodationId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("accommodation").get("id"), accommodationId);
        };
    }

    /**
     * Filter seasons that are global (no accommodation)
     */
    public static Specification<Season> isGlobalSeason() {
        return (root, query, cb) -> cb.isTrue(root.get("isGlobal"));
    }

    /**
     * Filter seasons that are accommodation-specific
     */
    public static Specification<Season> isAccommodationSeason() {
        return (root, query, cb) -> cb.isFalse(root.get("isGlobal"));
    }

    /**
     * Filter by description (case-insensitive partial match)
     */
    public static Specification<Season> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter active global seasons
     * Commonly used for park pricing, etc.
     */
    public static Specification<Season> activeGlobalSeasons() {
        return (root, query, cb) -> cb.and(
            cb.isTrue(root.get("isGlobal")),
            cb.isTrue(root.get("isActive"))
        );
    }

    /**
     * Filter system seasons only (protected from deletion)
     * Returns the three core seasons: High Season, Shoulder Season, Low Season
     */
    public static Specification<Season> systemSeasonsOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("isSystem"));
    }

    /**
     * Filter non-system seasons only (can be deleted)
     */
    public static Specification<Season> nonSystemSeasonsOnly() {
        return (root, query, cb) -> cb.isFalse(root.get("isSystem"));
    }

    /**
     * Filter active system seasons
     * Returns core active seasons for general use
     */
    public static Specification<Season> activeSystemSeasons() {
        return (root, query, cb) -> cb.and(
            cb.isTrue(root.get("isSystem")),
            cb.isTrue(root.get("isActive"))
        );
    }

    /**
     * Filter active accommodation-specific seasons for a specific accommodation
     */
    public static Specification<Season> activeAccommodationSeasons(Long accommodationId) {
        return (root, query, cb) -> {
            if (accommodationId == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.isFalse(root.get("isGlobal")),
                cb.isTrue(root.get("isActive")),
                cb.equal(root.get("accommodation").get("id"), accommodationId)
            );
        };
    }

    /**
     * Search across name and description fields
     */
    public static Specification<Season> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern)
            );
        };
    }
}
