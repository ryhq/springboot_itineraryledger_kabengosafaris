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
}
