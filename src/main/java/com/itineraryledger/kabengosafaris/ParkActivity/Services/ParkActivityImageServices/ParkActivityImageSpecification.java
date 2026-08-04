package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;

/**
 * JPA Specifications for ParkActivityImage filtering.
 */
public class ParkActivityImageSpecification {

    // ========================
    // PARK-ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityImage> byParkId(Long parkId) {
        return (root, query, cb) -> parkId == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("park").get("id"), parkId);
    }

    public static Specification<ParkActivityImage> byActivityId(Long activityId) {
        return (root, query, cb) -> activityId == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("id"), activityId);
    }

    public static Specification<ParkActivityImage> byParkActivity(Long parkId, Long activityId) {
        return (root, query, cb) -> {
            if (parkId == null || activityId == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.equal(root.get("parkActivity").get("park").get("id"), parkId),
                cb.equal(root.get("parkActivity").get("activity").get("id"), activityId)
            );
        };
    }

    // ========================
    // IMAGE SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityImage> byImageType(ImageType imageType) {
        return (root, query, cb) -> imageType == null
            ? cb.conjunction()
            : cb.equal(root.get("imageType"), imageType);
    }

    public static Specification<ParkActivityImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<ParkActivityImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ParkActivityImage> byDisplayOrder(Integer displayOrder) {
        return (root, query, cb) -> displayOrder == null
            ? cb.conjunction()
            : cb.equal(root.get("displayOrder"), displayOrder);
    }

    // ========================
    // PARK SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityImage> byParkName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("parkActivity").get("park").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkActivityImage> byParkIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("park").get("isActive"), isActive);
    }

    // ========================
    // ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityImage> byActivityName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("parkActivity").get("activity").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkActivityImage> byActivityIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("isActive"), isActive);
    }

    public static Specification<ParkActivityImage> byActivityHasTariff(Boolean hasTariff) {
        return (root, query, cb) -> hasTariff == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("hasTariff"), hasTariff);
    }

    /**
     * Free-text over the fields a person would recognise. The list page has always
     * shown a search box; without this it sent `keyword` into the void.
     */
    public static Specification<ParkActivityImage> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("caption")), like),
                cb.like(cb.lower(root.get("altText")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like)
            );
        };
    }

    /** Data-quality counters: every stat must also be reachable as a filter. */
    public static Specification<ParkActivityImage> missingCaption() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("caption")),
            cb.equal(cb.trim(root.get("caption")), "")
        );
    }

    public static Specification<ParkActivityImage> missingAltText() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("altText")),
            cb.equal(cb.trim(root.get("altText")), "")
        );
    }

    public static Specification<ParkActivityImage> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }

    /* Multi-value facets: OR inside a dimension, AND across dimensions. */

    public static Specification<ParkActivityImage> imageTypeIn(java.util.List<ImageType> types) {
        return (root, query, cb) ->
            types == null || types.isEmpty() ? cb.conjunction() : root.get("imageType").in(types);
    }

    /** Either quality problem, so one "data quality" facet can carry both. */
    public static Specification<ParkActivityImage> anyQualityIssue(boolean noCaption, boolean noAlt) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            if (noCaption) any.add(cb.or(
                cb.isNull(root.get("caption")), cb.equal(cb.trim(root.get("caption")), "")));
            if (noAlt) any.add(cb.or(
                cb.isNull(root.get("altText")), cb.equal(cb.trim(root.get("altText")), "")));
            return any.isEmpty() ? cb.conjunction() : cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static Specification<ParkActivityImage> createdBefore(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), moment);
    }

    /** Website visibility, so the "On website" card can both count and filter. */
    public static Specification<ParkActivityImage> isWebActive(Boolean isWebActive) {
        return (root, query, cb) ->
            isWebActive == null ? cb.conjunction() : cb.equal(root.get("isWebActive"), isWebActive);
    }
}
