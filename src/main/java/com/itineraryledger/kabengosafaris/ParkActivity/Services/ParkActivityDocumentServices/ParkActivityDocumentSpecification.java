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

    /** Free-text over the fields a person would recognise. */
    public static Specification<ParkActivityDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like),
                cb.like(cb.lower(root.get("version")), like)
            );
        };
    }

    /* Validity counters — warn BEFORE expiry, which is the point of tracking it. */

    public static Specification<ParkActivityDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), LocalDateTime.now())
        );
    }

    public static Specification<ParkActivityDocument> expiringWithin(int days) {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.greaterThanOrEqualTo(root.get("validTo"), LocalDateTime.now()),
            cb.lessThanOrEqualTo(root.get("validTo"), LocalDateTime.now().plusDays(days))
        );
    }

    public static Specification<ParkActivityDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }

    public static Specification<ParkActivityDocument> createdAfter(LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }

    /* Multi-value facets: OR inside a dimension, AND across dimensions. */

    public static Specification<ParkActivityDocument> documentTypeIn(java.util.List<DocumentType> types) {
        return (root, query, cb) ->
            types == null || types.isEmpty() ? cb.conjunction() : root.get("documentType").in(types);
    }

    /** Expired / expiring / no-expiry as ONE facet, so the cards can filter. */
    public static Specification<ParkActivityDocument> validityIn(java.util.List<String> validity) {
        return (root, query, cb) -> {
            if (validity == null || validity.isEmpty()) return cb.conjunction();
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            if (validity.contains("expired")) any.add(cb.and(
                cb.isNotNull(root.get("validTo")), cb.lessThan(root.get("validTo"), now)));
            if (validity.contains("expiringSoon")) any.add(cb.and(
                cb.isNotNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), now),
                cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(30))));
            if (validity.contains("noExpiry")) any.add(cb.isNull(root.get("validTo")));
            return any.isEmpty() ? cb.conjunction() : cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static Specification<ParkActivityDocument> createdBefore(LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), moment);
    }
}
