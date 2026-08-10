package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage.ImageType;

/**
 * JPA Specifications for ItineraryImage filtering.
 */
public class ItineraryImageSpecification {

    public static Specification<ItineraryImage> byItineraryId(Long itineraryId) {
        return (root, query, cb) -> itineraryId == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("id"), itineraryId);
    }

    public static Specification<ItineraryImage> byItineraryName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("itinerary").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ItineraryImage> byImageType(ImageType imageType) {
        return (root, query, cb) -> imageType == null
            ? cb.conjunction()
            : cb.equal(root.get("imageType"), imageType);
    }

    public static Specification<ItineraryImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<ItineraryImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ItineraryImage> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> isWebActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isWebActive"), isWebActive);
    }

    public static Specification<ItineraryImage> byDisplayOrder(Integer displayOrder) {
        return (root, query, cb) -> displayOrder == null
            ? cb.conjunction()
            : cb.equal(root.get("displayOrder"), displayOrder);
    }

    /**
     * Any of these types — the list page's Type filter is multi-select, and a
     * singular enum param cannot answer "hero or gallery".
     */
    public static Specification<ItineraryImage> byImageTypes(java.util.List<ImageType> types) {
        return (root, query, cb) -> types == null || types.isEmpty()
            ? cb.conjunction()
            : root.get("imageType").in(types);
    }

    /* Data-quality counters: every stat card must also work as a filter. */

    public static Specification<ItineraryImage> missingCaption() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("caption")), cb.equal(cb.trim(root.get("caption")), ""));
    }

    public static Specification<ItineraryImage> missingAltText() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("altText")), cb.equal(cb.trim(root.get("altText")), ""));
    }

    public static Specification<ItineraryImage> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    /** The one search box: what an image is recognised by. */
    public static Specification<ItineraryImage> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("caption")), like),
                cb.like(cb.lower(root.get("altText")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like));
        };
    }
}
