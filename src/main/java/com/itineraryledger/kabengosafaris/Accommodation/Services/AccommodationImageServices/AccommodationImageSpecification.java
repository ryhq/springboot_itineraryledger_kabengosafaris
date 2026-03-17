package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import org.springframework.data.jpa.domain.Specification;

/**
 * AccommodationImageSpecification - Dynamic JPA Specifications for filtering AccommodationImage entities
 */
public class AccommodationImageSpecification {

    private AccommodationImageSpecification() {
    }

    // ========================
    // IMAGE SPECIFICATIONS
    // ========================

    public static Specification<AccommodationImage> byAccommodationId(Long accommodationId) {
        return (root, query, cb) -> accommodationId == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    public static Specification<AccommodationImage> byImageType(ImageType imageType) {
        return (root, query, cb) -> imageType == null
            ? cb.conjunction()
            : cb.equal(root.get("imageType"), imageType);
    }

    public static Specification<AccommodationImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<AccommodationImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<AccommodationImage> isWebActive(Boolean isWebActive) {
        return (root, query, cb) -> isWebActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isWebActive"), isWebActive);
    }

    public static Specification<AccommodationImage> byDisplayOrder(Integer displayOrder) {
        return (root, query, cb) -> displayOrder == null
            ? cb.conjunction()
            : cb.equal(root.get("displayOrder"), displayOrder);
    }

    // ========================
    // ACCOMMODATION SPECIFICATIONS
    // ========================

    public static Specification<AccommodationImage> byAccommodationName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("accommodation").get("name")),
                "%" + name.trim().toLowerCase() + "%");
        };
    }

    public static Specification<AccommodationImage> byAccommodationType(AccommodationType accommodationType) {
        return (root, query, cb) -> accommodationType == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("accommodationType"), accommodationType);
    }

    public static Specification<AccommodationImage> byAccommodationCategory(AccommodationCategory category) {
        return (root, query, cb) -> category == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("category"), category);
    }
}
