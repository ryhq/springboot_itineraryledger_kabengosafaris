package com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Park.ParkType;

/**
 * JPA Specifications for ParkDocument filtering.
 */
public class ParkDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<ParkDocument> byParkId(Long parkId) {
        return (root, query, cb) -> parkId == null
            ? cb.conjunction()
            : cb.equal(root.get("park").get("id"), parkId);
    }

    public static Specification<ParkDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<ParkDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ParkDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkDocument> byCurrentlyValid(LocalDateTime date) {
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

    public static Specification<ParkDocument> byTariffDocument() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.TARIFF),
            cb.equal(root.get("documentType"), DocumentType.FEE_SCHEDULE)
        );
    }

    // ========================
    // PARK SPECIFICATIONS
    // ========================

    public static Specification<ParkDocument> byParkName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("park").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ParkDocument> byParkType(ParkType parkType) {
        return (root, query, cb) -> parkType == null
            ? cb.conjunction()
            : cb.equal(root.get("park").get("parkType"), parkType);
    }

    public static Specification<ParkDocument> byParkRegion(String region) {
        return (root, query, cb) -> {
            if (region == null || region.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("park").get("region")), "%" + region.toLowerCase().trim() + "%");
        };
    }

    /* ---- stat-card support: every counter below is also a filter ---- */

    public static Specification<ParkDocument> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    /** Past its valid-to date. */
    public static Specification<ParkDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), java.time.LocalDateTime.now())
        );
    }

    /** Expires within the next N days (and has not expired yet). */
    public static Specification<ParkDocument> expiringWithin(int days) {
        return (root, query, cb) -> {
            var now = java.time.LocalDateTime.now();
            return cb.and(
                cb.isNotNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), now),
                cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(days))
            );
        };
    }

    /** Open-ended — no valid-to recorded. */
    public static Specification<ParkDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }

    /** Any of the given document types (OR within the dimension). */
    public static Specification<ParkDocument> documentTypeIn(java.util.List<ParkDocument.DocumentType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("documentType").in(types);
        };
    }

    public static Specification<ParkDocument> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    /** OR of the requested validity states, so those cards filter. */
    public static Specification<ParkDocument> anyValidityState(boolean isExpired, boolean isExpiring, boolean hasNoExpiry) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            if (isExpired) any.add(expired().toPredicate(root, query, cb));
            if (isExpiring) any.add(expiringWithin(30).toPredicate(root, query, cb));
            if (hasNoExpiry) any.add(noExpiry().toPredicate(root, query, cb));
            any.removeIf(java.util.Objects::isNull);
            if (any.isEmpty()) return cb.conjunction();
            return cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Free-text search over title and both filenames — the fields a person would
     * recognise. Without it the list's search box sent a parameter nothing read.
     */
    public static Specification<ParkDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like)
            );
        };
    }
}
