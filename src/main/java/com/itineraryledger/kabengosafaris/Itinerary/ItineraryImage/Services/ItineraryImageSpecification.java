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
}
