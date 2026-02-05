package com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;

/**
 * JPA Specifications for SafariDocument filtering.
 */
public class SafariDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<SafariDocument> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null
            ? cb.conjunction()
            : cb.equal(root.get("safari").get("id"), safariId);
    }

    public static Specification<SafariDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<SafariDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<SafariDocument> byIsGenerated(Boolean isGenerated) {
        return (root, query, cb) -> isGenerated == null
            ? cb.conjunction()
            : cb.equal(root.get("isGenerated"), isGenerated);
    }

    public static Specification<SafariDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<SafariDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<SafariDocument> byCurrentlyValid(LocalDateTime date) {
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

    public static Specification<SafariDocument> byQuotationDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.QUOTATION),
            cb.equal(root.get("documentType"), DocumentType.PROFORMA_INVOICE),
            cb.equal(root.get("documentType"), DocumentType.INVOICE)
        );
    }

    public static Specification<SafariDocument> byTravelDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.TRAVEL_PLAN),
            cb.equal(root.get("documentType"), DocumentType.FINAL_ITINERARY),
            cb.equal(root.get("documentType"), DocumentType.FLIGHT_ITINERARY)
        );
    }

    public static Specification<SafariDocument> byVoucherDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.ACCOMMODATION_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.ACTIVITY_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.TRANSFER_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.PARK_PERMITS)
        );
    }

    // ========================
    // SAFARI SPECIFICATIONS
    // ========================

    public static Specification<SafariDocument> bySafariName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("safari").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<SafariDocument> bySafariCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("safari").get("code")), "%" + code.toLowerCase().trim() + "%");
        };
    }

    public static Specification<SafariDocument> bySafariIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("safari").get("isActive"), isActive);
    }

    public static Specification<SafariDocument> bySafariState(SafariState state) {
        return (root, query, cb) -> state == null
            ? cb.conjunction()
            : cb.equal(root.get("safari").get("state"), state);
    }
}
