package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;

/**
 * JPA Specifications for AccommodationDocument filtering.
 */
public class AccommodationDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<AccommodationDocument> byAccommodationId(Long accommodationId) {
        return (root, query, cb) -> accommodationId == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    public static Specification<AccommodationDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<AccommodationDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<AccommodationDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<AccommodationDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<AccommodationDocument> byCurrentlyValid(LocalDateTime date) {
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

    public static Specification<AccommodationDocument> byValidFrom(LocalDateTime validFrom) {
        return (root, query, cb) -> validFrom == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("validFrom"), validFrom);
    }

    public static Specification<AccommodationDocument> byValidTo(LocalDateTime validTo) {
        return (root, query, cb) -> validTo == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("validTo"), validTo);
    }

    public static Specification<AccommodationDocument> byRateDocument() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.STO_RATE),
            cb.equal(root.get("documentType"), DocumentType.RACK_RATE)
        );
    }

    // ========================
    // ACCOMMODATION SPECIFICATIONS
    // ========================

    public static Specification<AccommodationDocument> byAccommodationName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("accommodation").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<AccommodationDocument> byAccommodationType(AccommodationType accommodationType) {
        return (root, query, cb) -> accommodationType == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("accommodationType"), accommodationType);
    }

    public static Specification<AccommodationDocument> byAccommodationCategory(AccommodationCategory category) {
        return (root, query, cb) -> category == null
            ? cb.conjunction()
            : cb.equal(root.get("accommodation").get("category"), category);
    }

    /* Validity, recency and free-text — warn BEFORE expiry, which is the point. */

    public static Specification<AccommodationDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), java.time.LocalDateTime.now()));
    }

    public static Specification<AccommodationDocument> expiringWithin(int days) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.greaterThanOrEqualTo(root.get("validTo"), now),
            cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(days)));
    }

    public static Specification<AccommodationDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }

    public static Specification<AccommodationDocument> createdAfter(java.time.LocalDateTime moment) {
        return (root, query, cb) ->
            moment == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), moment);
    }

    public static Specification<AccommodationDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like));
        };
    }
}
