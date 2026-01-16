package com.itineraryledger.kabengosafaris.Activity;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;

/**
 * ActivitySpecification - Provides reusable Specification objects for filtering Activity entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<Activity> that can be combined with other specifications
 */
public class ActivitySpecification {

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<Activity> nameLike(String name) {
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
    public static Specification<Activity> slugLike(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("slug")), "%" + slug.toLowerCase() + "%");
        };
    }

    /**
     * Filter by hasTariff status
     */
    public static Specification<Activity> hasTariff(Boolean hasTariff) {
        return (root, query, cb) -> {
            if (hasTariff == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("hasTariff"), hasTariff);
        };
    }

    /**
     * Filter by web active status
     */
    public static Specification<Activity> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> {
            if (isWebActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isWebActive"), isWebActive);
        };
    }

    /**
     * Filter by charging basis
     */
    public static Specification<Activity> hasChargingBasis(ChargingBasis chargingBasis) {
        return (root, query, cb) -> {
            if (chargingBasis == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("chargingBasis"), chargingBasis);
        };
    }

    /**
     * Filter by description (case-insensitive partial match)
     */
    public static Specification<Activity> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter by season availability (case-insensitive partial match)
     */
    public static Specification<Activity> seasonAvailabilityLike(String seasonAvailability) {
        return (root, query, cb) -> {
            if (seasonAvailability == null || seasonAvailability.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("seasonAvailability")), "%" + seasonAvailability.toLowerCase() + "%");
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<Activity> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Search across multiple text fields (name, description, tags, equipmentRequired)
     * Useful for general search functionality
     */
    public static Specification<Activity> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), likePattern),
                cb.like(cb.lower(root.get("description")), likePattern),
                cb.like(cb.lower(root.get("tags")), likePattern),
                cb.like(cb.lower(root.get("equipmentRequired")), likePattern),
                cb.like(cb.lower(root.get("safetyInformation")), likePattern)
            );
        };
    }

    /**
     * Filter activities by park ID
     * Returns activities that are associated with the specified park
     */
    public static Specification<Activity> byParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.conjunction();
            }
            // Join with ParkActivity table
            Join<Activity, ParkActivity> parkActivityJoin = root.join("parkActivities", JoinType.INNER);
            return cb.equal(parkActivityJoin.get("park").get("id"), parkId);
        };
    }

    /**
     * Filter activities NOT associated with a specific park ID
     * Returns activities that are NOT assigned to the specified park
     */
    public static Specification<Activity> notByParkId(Long parkId) {
        return (root, query, cb) -> {
            if (parkId == null) {
                return cb.conjunction();
            }
            // Subquery to get activity IDs that are assigned to this park
            var subquery = query.subquery(Long.class);
            var subRoot = subquery.from(Activity.class);
            var subJoin = subRoot.join("parkActivities", JoinType.INNER);
            subquery.select(subRoot.get("id"))
                    .where(cb.equal(subJoin.get("park").get("id"), parkId));

            // Return activities whose ID is NOT in the subquery result
            return cb.not(root.get("id").in(subquery));
        };
    }

    /**
     * Filter by standalone status
     * Standalone activities are NOT linked to any park (not in parks_activities table)
     *
     * @param isStandalone true = activities with no park associations, false = activities with at least one park association
     */
    public static Specification<Activity> isStandalone(Boolean isStandalone) {
        return (root, query, cb) -> {
            if (isStandalone == null) {
                return cb.conjunction();
            }

            // Subquery to get activity IDs that are linked to any park
            var subquery = query.subquery(Long.class);
            var activityRoot = subquery.from(Activity.class);
            var parkActivitiesJoin = activityRoot.join("parkActivities", JoinType.INNER);

            // Select activity IDs that have park associations (using the join)
            subquery.select(parkActivitiesJoin.get("activity").get("id"));

            if (isStandalone) {
                // Return activities whose ID is NOT in the subquery (standalone activities)
                return cb.not(root.get("id").in(subquery));
            } else {
                // Return activities whose ID IS in the subquery (park-linked activities)
                return root.get("id").in(subquery);
            }
        };
    }
}
