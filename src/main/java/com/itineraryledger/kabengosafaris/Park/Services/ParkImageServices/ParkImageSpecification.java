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
}
