package com.itineraryledger.kabengosafaris.Park;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;

/**
 * ParkSpecification - Provides reusable Specification objects for filtering Park entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Park> that can be combined with other specifications
 */
public class ParkSpecification {

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<Park> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by slug (case-insensitive partial match)
     */
    public static Specification<Park> slugLike(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("slug")), "%" + slug.toLowerCase() + "%");
        };
    }

    /**
     * Filter by park type
     */
    public static Specification<Park> hasParkType(ParkType parkType) {
        return (root, query, cb) -> {
            if (parkType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("parkType"), parkType);
        };
    }

    /**
     * Filter by region (case-insensitive partial match)
     */
    public static Specification<Park> regionLike(String region) {
        return (root, query, cb) -> {
            if (region == null || region.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("region")), "%" + region.toLowerCase() + "%");
        };
    }

    /**
     * Filter by district (case-insensitive partial match)
     */
    public static Specification<Park> districtLike(String district) {
        return (root, query, cb) -> {
            if (district == null || district.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("district")), "%" + district.toLowerCase() + "%");
        };
    }

    /**
     * Filter by location (case-insensitive partial match)
     */
    public static Specification<Park> locationLike(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
        };
    }

    /**
     * Filter by size (exact match)
     */
    public static Specification<Park> hasSize(String size) {
        return (root, query, cb) -> {
            if (size == null || size.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("size"), size);
        };
    }

    public static Specification<Park> sizeLike(String size) {
        return (root, query, cb) -> {
            if (size == null || size.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("size")), "%" + size.toLowerCase() + "%");
        };
    }

    /**
     * Filter by elevation (exact match)
     */
    public static Specification<Park> hasElevation(String elevation) {
        return (root, query, cb) -> {
            if (elevation == null || elevation.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("elevation"), elevation);
        };
    }

    /**
     * Filter by short description (case-insensitive partial match)
     */
    public static Specification<Park> shortDescriptionLike(String shortDescription) {
        return (root, query, cb) -> {
            if (shortDescription == null || shortDescription.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("shortDescription")), "%" + shortDescription.toLowerCase() + "%");
        };
    }

    /**
     * Filter by best time to visit (case-insensitive partial match)
     */
    public static Specification<Park> bestTimeToVisitLike(String bestTimeToVisit) {
        return (root, query, cb) -> {
            if (bestTimeToVisit == null || bestTimeToVisit.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("bestTimeToVisit")), "%" + bestTimeToVisit.toLowerCase() + "%");
        };
    }

    /**
     * Filter by opening hours (case-insensitive partial match)
     */
    public static Specification<Park> openingHoursLike(String openingHours) {
        return (root, query, cb) -> {
            if (openingHours == null || openingHours.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("openingHours")), "%" + openingHours.toLowerCase() + "%");
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Park> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by web active status
     */
    public static Specification<Park> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> {
            if (isWebActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isWebActive"), isWebActive);
        };
    }

    /**
     * Search across multiple text fields (name, short description, region, district, location)
     * Useful for general search functionality
     */
    public static Specification<Park> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("shortDescription")), likePattern),
                cb.like(cb.lower(root.get("region")), likePattern),
                cb.like(cb.lower(root.get("district")), likePattern),
                cb.like(cb.lower(root.get("location")), likePattern),
                cb.like(cb.lower(root.get("tags").as(String.class)), likePattern)
            );
        };
    }

    /**
     * Filter parks by activity ID
     * Returns parks that offer the specified activity
     */
    public static Specification<Park> byActivityId(Long activityId) {
        return (root, query, cb) -> {
            if (activityId == null) {
                return cb.conjunction();
            }
            // Join with ParkActivity table
            Join<Park, ParkActivity> parkActivityJoin = root.join("parkActivities", JoinType.INNER);
            return cb.equal(parkActivityJoin.get("activity").get("id"), activityId);
        };
    }

    /**
     * Filter parks NOT associated with a specific activity ID
     * Returns parks that do NOT offer the specified activity
     */
    public static Specification<Park> notByActivityId(Long activityId) {
        return (root, query, cb) -> {
            if (activityId == null) {
                return cb.conjunction();
            }
            // Subquery to get park IDs that offer this activity
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(Park.class);
            var subJoin = subRoot.join("parkActivities", JoinType.INNER);
            subquery.select(subRoot.get("id"))
                    .where(cb.equal(subJoin.get("activity").get("id"), activityId));

            // Return parks whose ID is NOT in the subquery result
            return cb.not(root.get("id").in(subquery));
        };
    }

    /**
     * Filter parks by tariff ID
     * Returns parks that have the specified tariff assigned
     */
    public static Specification<Park> byTariffId(Long tariffId) {
        return (root, query, cb) -> {
            if (tariffId == null) {
                return cb.conjunction();
            }
            // Join with ParkTariff table
            Join<Park, ParkTariff> parkTariffJoin = root.join("parkTariffs", JoinType.INNER);
            return cb.equal(parkTariffJoin.get("tariff").get("id"), tariffId);
        };
    }

    /**
     * Filter parks NOT associated with a specific tariff ID
     * Returns parks that do NOT have the specified tariff assigned
     */
    public static Specification<Park> notByTariffId(Long tariffId) {
        return (root, query, cb) -> {
            if (tariffId == null) {
                return cb.conjunction();
            }
            // Subquery to get park IDs that have this tariff
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(Park.class);
            var subJoin = subRoot.join("parkTariffs", JoinType.INNER);
            subquery.select(subRoot.get("id"))
                    .where(cb.equal(subJoin.get("tariff").get("id"), tariffId));

            // Return parks whose ID is NOT in the subquery result
            return cb.not(root.get("id").in(subquery));
        };
    }
    /* ------------------------------------------------------------------
     * Multi-value facets: OR inside a dimension, AND across dimensions.
     * The list rows and the stat counters both build on these, so a card
     * and the table it heads can never disagree.
     * ------------------------------------------------------------------ */

    /** Any of the given park types. */
    public static Specification<Park> parkTypeIn(java.util.List<ParkType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("parkType").in(types);
        };
    }

    /** Any of the given active states; passing both cancels to no constraint. */
    public static Specification<Park> activeIn(java.util.List<Boolean> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty() || states.size() > 1) return cb.conjunction();
            return cb.equal(root.get("isActive"), states.get(0));
        };
    }

    /** Any of the given website-visibility states. */
    public static Specification<Park> webActiveIn(java.util.List<Boolean> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty() || states.size() > 1) return cb.conjunction();
            return cb.equal(root.get("isWebActive"), states.get(0));
        };
    }

    /** Any of the given regions (exact, case-insensitive). */
    public static Specification<Park> regionIn(java.util.List<String> regions) {
        return (root, query, cb) -> {
            if (regions == null || regions.isEmpty()) return cb.conjunction();
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            for (String region : regions) {
                if (region != null && !region.isBlank()) {
                    any.add(cb.equal(cb.lower(root.get("region")), region.toLowerCase()));
                }
            }
            return any.isEmpty() ? cb.conjunction() : cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /** Parks created on or after the given instant. */
    public static Specification<Park> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    /** Parks created on or before the given instant. */
    public static Specification<Park> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    /** True = has at least one image; false = has none. */
    public static Specification<Park> hasImages(Boolean has) {
        return (root, query, cb) -> {
            if (has == null) return cb.conjunction();
            var sub = query.subquery(Long.class);
            var image = sub.from(com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.class);
            sub.select(cb.count(image)).where(cb.equal(image.get("park"), root));
            return has ? cb.greaterThan(sub, 0L) : cb.equal(sub, 0L);
        };
    }

    /** True = priced (has at least one tariff); false = unpriced. */
    public static Specification<Park> hasTariffs(Boolean has) {
        return (root, query, cb) -> {
            if (has == null) return cb.conjunction();
            var sub = query.subquery(Long.class);
            var tariff = sub.from(ParkTariff.class);
            sub.select(cb.count(tariff)).where(cb.equal(tariff.get("park"), root));
            return has ? cb.greaterThan(sub, 0L) : cb.equal(sub, 0L);
        };
    }

    /**
     * Actionable data-quality gaps — every one of these is also a stat card, so
     * a count the admin sees is always reachable as a filter.
     */
    public static Specification<Park> anyQualityIssue(
        boolean missingDescription,
        boolean missingImage,
        boolean missingCoordinates,
        boolean missingTariff
    ) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            if (missingDescription) {
                any.add(cb.or(
                    cb.isNull(root.get("shortDescription")),
                    cb.equal(cb.trim(root.get("shortDescription").as(String.class)), "")
                ));
            }
            if (missingImage) {
                var sub = query.subquery(Long.class);
                var image = sub.from(com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.class);
                sub.select(cb.count(image)).where(cb.equal(image.get("park"), root));
                any.add(cb.equal(sub, 0L));
            }
            if (missingCoordinates) {
                any.add(cb.or(cb.isNull(root.get("latitude")), cb.isNull(root.get("longitude"))));
            }
            if (missingTariff) {
                var sub = query.subquery(Long.class);
                var tariff = sub.from(ParkTariff.class);
                sub.select(cb.count(tariff)).where(cb.equal(tariff.get("park"), root));
                any.add(cb.equal(sub, 0L));
            }
            if (any.isEmpty()) return cb.conjunction();
            query.distinct(true);
            return cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /** Match parks whose comma/JSON tags string contains the given tag (case-insensitive). */
    public static Specification<Park> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return cb.conjunction();
            // tags is @Lob/TEXT (CLOB) — cast to String so LIKE/LOWER work in Criteria.
            return cb.like(cb.lower(root.get("tags").as(String.class)), "%" + tag.toLowerCase() + "%");
        };
    }
}
