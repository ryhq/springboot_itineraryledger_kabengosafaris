package com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;
import com.itineraryledger.kabengosafaris.Park.ParkType;

/**
 * JPA Specifications for ParkImage filtering.
 */
public class ParkImageSpecification {

    // ========================
    // IMAGE SPECIFICATIONS
    // ========================

    public static Specification<ParkImage> byParkId(Long parkId) {
        return (root, query, cb) -> parkId == null
            ? cb.conjunction()
            : cb.equal(root.get("park").get("id"), parkId);
    }

    public static Specification<ParkImage> byImageType(ImageType imageType) {
        return (root, query, cb) -> imageType == null
            ? cb.conjunction()
            : cb.equal(root.get("imageType"), imageType);
    }

    public static Specification<ParkImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<ParkImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ParkImage> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> isWebActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isWebActive"), isWebActive);
    }

    public static Specification<ParkImage> byDisplayOrder(Integer displayOrder) {
        return (root, query, cb) -> displayOrder == null
            ? cb.conjunction()
            : cb.equal(root.get("displayOrder"), displayOrder);
    }

    // ========================
    // PARK SPECIFICATIONS
    // ========================

    public static Specification<ParkImage> byParkName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("park").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkImage> byParkType(ParkType parkType) {
        return (root, query, cb) -> parkType == null
            ? cb.conjunction()
            : cb.equal(root.get("park").get("parkType"), parkType);
    }

    public static Specification<ParkImage> byParkRegion(String region) {
        return (root, query, cb) -> {
            if (region == null || region.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("park").get("region")), "%" + region.toLowerCase().trim() + "%");
        };
    }

    /* ---- stat-card support: every counter below is also a filter ---- */

    public static Specification<ParkImage> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    /** No caption — the image cannot be labelled on the website. */
    public static Specification<ParkImage> missingCaption() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("caption")),
            cb.equal(cb.trim(root.get("caption").as(String.class)), "")
        );
    }

    /** No alt text — an accessibility gap. */
    public static Specification<ParkImage> missingAltText() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("altText")),
            cb.equal(cb.trim(root.get("altText").as(String.class)), "")
        );
    }

    /** Any of the given image types (OR within the dimension). */
    public static Specification<ParkImage> imageTypeIn(java.util.List<ParkImage.ImageType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("imageType").in(types);
        };
    }

    public static Specification<ParkImage> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    /** OR of the requested gaps, so the "No caption"/"No alt text" cards filter. */
    public static Specification<ParkImage> anyQualityIssue(boolean noCaption, boolean noAlt) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            if (noCaption) any.add(missingCaption().toPredicate(root, query, cb));
            if (noAlt) any.add(missingAltText().toPredicate(root, query, cb));
            any.removeIf(java.util.Objects::isNull);
            if (any.isEmpty()) return cb.conjunction();
            return cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Free-text search over the fields a person would recognise: the caption, the
     * alt text and both filenames. The list page has always offered a search box;
     * without this the parameter was ignored, which is worse than no search.
     */
    public static Specification<ParkImage> searchKeyword(String keyword) {
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
}
