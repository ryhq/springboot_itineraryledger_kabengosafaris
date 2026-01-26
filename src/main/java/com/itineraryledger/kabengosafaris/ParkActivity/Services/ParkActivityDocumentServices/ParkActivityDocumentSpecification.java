package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;

/**
 * JPA Specifications for ParkActivityDocument filtering.
 */
public class ParkActivityDocumentSpecification {

    // ========================
    // PARK-ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityDocument> byParkId(Long parkId) {
        return (root, query, cb) -> parkId == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("park").get("id"), parkId);
    }

    public static Specification<ParkActivityDocument> byActivityId(Long activityId) {
        return (root, query, cb) -> activityId == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("id"), activityId);
    }

    public static Specification<ParkActivityDocument> byParkActivity(Long parkId, Long activityId) {
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
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<ParkActivityDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ParkActivityDocument> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkActivityDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("version"), version.trim());
        };
    }

    public static Specification<ParkActivityDocument> currentlyValid(LocalDateTime date) {
        final LocalDateTime checkDate = date != null ? date : LocalDateTime.now();
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("isActive"), true),
            cb.or(
                cb.isNull(root.get("validFrom")),
                cb.lessThanOrEqualTo(root.get("validFrom"), checkDate)
            ),
            cb.or(
                cb.isNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), checkDate)
            )
        );
    }

    // ========================
    // PARK SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityDocument> byParkName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("parkActivity").get("park").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkActivityDocument> byParkIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("park").get("isActive"), isActive);
    }

    // ========================
    // ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ParkActivityDocument> byActivityName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("parkActivity").get("activity").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkActivityDocument> byActivityIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("isActive"), isActive);
    }

    public static Specification<ParkActivityDocument> byActivityHasTariff(Boolean hasTariff) {
        return (root, query, cb) -> hasTariff == null
            ? cb.conjunction()
            : cb.equal(root.get("parkActivity").get("activity").get("hasTariff"), hasTariff);
    }
}
