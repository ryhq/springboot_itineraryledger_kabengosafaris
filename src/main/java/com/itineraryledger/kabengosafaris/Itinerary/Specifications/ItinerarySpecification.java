package com.itineraryledger.kabengosafaris.Itinerary.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;

/**
 * ItinerarySpecification - JPA Specifications for dynamic filtering of itineraries
 */
public class ItinerarySpecification {

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<Itinerary> nameLike(String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filter by code (case-insensitive partial match)
     */
    public static Specification<Itinerary> codeLike(String code) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    /**
     * Filter by status
     */
    public static Specification<Itinerary> hasStatus(ItineraryStatus status) {
        return (root, query, cb) ->
            cb.equal(root.get("status"), status);
    }

    /**
     * Filter by start location (case-insensitive partial match)
     */
    public static Specification<Itinerary> startLocationLike(String startLocation) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("startLocation")), "%" + startLocation.toLowerCase() + "%");
    }

    /**
     * Filter by end location (case-insensitive partial match)
     */
    public static Specification<Itinerary> endLocationLike(String endLocation) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("endLocation")), "%" + endLocation.toLowerCase() + "%");
    }

    /**
     * Filter by exact total days
     */
    public static Specification<Itinerary> hasTotalDays(Integer totalDays) {
        return (root, query, cb) ->
            cb.equal(root.get("totalDays"), totalDays);
    }

    /**
     * Filter by minimum total days
     */
    public static Specification<Itinerary> minTotalDays(Integer minDays) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("totalDays"), minDays);
    }

    /**
     * Filter by maximum total days
     */
    public static Specification<Itinerary> maxTotalDays(Integer maxDays) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("totalDays"), maxDays);
    }

    /**
     * Filter by active status
     */
    public static Specification<Itinerary> isActive(Boolean isActive) {
        return (root, query, cb) ->
            cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Filter by created by user
     */
    public static Specification<Itinerary> createdBy(Long userId) {
        return (root, query, cb) ->
            cb.equal(root.get("createdBy"), userId);
    }

    /**
     * Search keyword across multiple fields (name, code, description, highlights)
     */
    public static Specification<Itinerary> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            // Note: description and highlights are @Lob fields — LOWER() on CLOB types
            // causes errors in Hibernate 6, so they are excluded from keyword search
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("startLocation")), pattern),
                cb.like(cb.lower(root.get("endLocation")), pattern)
            );
        };
    }

    /**
     * Filter by status not equal to
     */
    public static Specification<Itinerary> statusNotEqual(ItineraryStatus status) {
        return (root, query, cb) ->
            cb.notEqual(root.get("status"), status);
    }

    /**
     * Filter for publishable itineraries (DRAFT or COMPLETE)
     */
    public static Specification<Itinerary> isPublishable() {
        return (root, query, cb) ->
            cb.or(
                cb.equal(root.get("status"), ItineraryStatus.DRAFT),
                cb.equal(root.get("status"), ItineraryStatus.COMPLETE)
            );
    }

    /**
     * Filter for active and published itineraries (typically for customer-facing queries)
     */
    public static Specification<Itinerary> isActiveAndPublished() {
        return (root, query, cb) ->
            cb.and(
                cb.equal(root.get("isActive"), true),
                cb.equal(root.get("status"), ItineraryStatus.PUBLISHED)
            );
    }

    /**
     * Filter by day trip (totalDays == 1 && totalNights == 0)
     */
    public static Specification<Itinerary> isDayTrip(Boolean isDayTrip) {
        return (root, query, cb) -> {
            if (isDayTrip) {
                return cb.and(
                    cb.equal(root.get("totalDays"), 1),
                    cb.equal(root.get("totalNights"), 0)
                );
            } else {
                return cb.or(
                    cb.notEqual(root.get("totalDays"), 1),
                    cb.notEqual(root.get("totalNights"), 0)
                );
            }
        };
    }

    /**
     * Filter by trip type
     */
    public static Specification<Itinerary> hasTripType(TripType tripType) {
        return (root, query, cb) ->
            cb.equal(root.get("tripType"), tripType);
    }

    /**
     * Filter by budget category
     */
    public static Specification<Itinerary> hasBudgetCategory(BudgetCategory budgetCategory) {
        return (root, query, cb) ->
            cb.equal(root.get("budgetCategory"), budgetCategory);
    }

    /**
     * Filter by the editor-curated "featured" flag
     */
    public static Specification<Itinerary> isFeatured(Boolean featured) {
        return (root, query, cb) ->
            featured == null ? cb.conjunction() : cb.equal(root.get("featured"), featured);
    }
}
