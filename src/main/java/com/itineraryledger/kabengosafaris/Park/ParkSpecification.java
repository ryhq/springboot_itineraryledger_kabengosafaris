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
            return cb.equal(root.get("isActive"), isActive);
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
                cb.like(cb.lower(root.get("location")), likePattern)
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
}
