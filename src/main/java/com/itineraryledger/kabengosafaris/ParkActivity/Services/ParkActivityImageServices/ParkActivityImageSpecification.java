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
}
