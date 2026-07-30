package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;

/**
 * JPA Specifications for ActivityImage filtering.
 */
public class ActivityImageSpecification {

    // ========================
    // IMAGE SPECIFICATIONS
    // ========================

    public static Specification<ActivityImage> byActivityId(Long activityId) {
        return (root, query, cb) -> activityId == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("id"), activityId);
    }

    public static Specification<ActivityImage> byImageType(ImageType imageType) {
        return (root, query, cb) -> imageType == null
            ? cb.conjunction()
            : cb.equal(root.get("imageType"), imageType);
    }

    public static Specification<ActivityImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<ActivityImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ActivityImage> byDisplayOrder(Integer displayOrder) {
        return (root, query, cb) -> displayOrder == null
            ? cb.conjunction()
            : cb.equal(root.get("displayOrder"), displayOrder);
    }

    // ========================
    // ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ActivityImage> byActivityName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("activity").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ActivityImage> byActivityIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("isActive"), isActive);
    }

    public static Specification<ActivityImage> byActivityHasTariff(Boolean hasTariff) {
        return (root, query, cb) -> hasTariff == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("hasTariff"), hasTariff);
    }

    /* ---- stat-card support: every counter below is also a filter ---- */

    public static Specification<ActivityImage> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    /** No caption — the image cannot be labelled on the website. */
    public static Specification<ActivityImage> missingCaption() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("caption")),
            cb.equal(cb.trim(root.get("caption").as(String.class)), "")
        );
    }

    /** No alt text — an accessibility gap. */
    public static Specification<ActivityImage> missingAltText() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("altText")),
            cb.equal(cb.trim(root.get("altText").as(String.class)), "")
        );
    }

    /** Any of the given image types (OR within the dimension). */
    public static Specification<ActivityImage> imageTypeIn(java.util.List<ActivityImage.ImageType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("imageType").in(types);
        };
    }

    public static Specification<ActivityImage> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    /** OR of the requested gaps, so the "No caption"/"No alt text" cards filter. */
    public static Specification<ActivityImage> anyQualityIssue(boolean noCaption, boolean noAlt) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            if (noCaption) any.add(missingCaption().toPredicate(root, query, cb));
            if (noAlt) any.add(missingAltText().toPredicate(root, query, cb));
            any.removeIf(java.util.Objects::isNull);
            if (any.isEmpty()) return cb.conjunction();
            return cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
