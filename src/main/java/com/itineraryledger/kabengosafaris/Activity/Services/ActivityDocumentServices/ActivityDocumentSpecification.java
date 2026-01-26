package com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;

/**
 * JPA Specifications for ActivityDocument filtering.
 */
public class ActivityDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<ActivityDocument> byActivityId(Long activityId) {
        return (root, query, cb) -> activityId == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("id"), activityId);
    }

    public static Specification<ActivityDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<ActivityDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ActivityDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ActivityDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ActivityDocument> byCurrentlyValid(LocalDateTime date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.equal(root.get("isActive"), true),
                cb.or(
                    cb.isNull(root.get("validFrom")),
                    cb.lessThanOrEqualTo(root.get("validFrom"), date)
                ),
                cb.or(
                    cb.isNull(root.get("validTo")),
                    cb.greaterThanOrEqualTo(root.get("validTo"), date)
                )
            );
        };
    }

    public static Specification<ActivityDocument> bySafetyDocument() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.SAFETY_GUIDELINES),
            cb.equal(root.get("documentType"), DocumentType.WAIVER),
            cb.equal(root.get("documentType"), DocumentType.LIABILITY_FORM),
            cb.equal(root.get("documentType"), DocumentType.EMERGENCY_PROCEDURE)
        );
    }

    // ========================
    // ACTIVITY SPECIFICATIONS
    // ========================

    public static Specification<ActivityDocument> byActivityName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("activity").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ActivityDocument> byActivityIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("isActive"), isActive);
    }

    public static Specification<ActivityDocument> byActivityHasTariff(Boolean hasTariff) {
        return (root, query, cb) -> hasTariff == null
            ? cb.conjunction()
            : cb.equal(root.get("activity").get("hasTariff"), hasTariff);
    }
}
