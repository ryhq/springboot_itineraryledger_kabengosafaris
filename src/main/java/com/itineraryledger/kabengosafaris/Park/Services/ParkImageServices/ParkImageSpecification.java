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
}
