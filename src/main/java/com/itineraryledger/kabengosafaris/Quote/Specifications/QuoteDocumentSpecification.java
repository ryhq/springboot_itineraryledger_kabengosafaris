package com.itineraryledger.kabengosafaris.Quote.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;

/**
 * JPA Specifications for QuoteDocument filtering.
 */
public class QuoteDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<QuoteDocument> byQuoteId(Long quoteId) {
        return (root, query, cb) -> quoteId == null
            ? cb.conjunction()
            : cb.equal(root.get("quote").get("id"), quoteId);
    }

    public static Specification<QuoteDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<QuoteDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<QuoteDocument> byIsGenerated(Boolean isGenerated) {
        return (root, query, cb) -> isGenerated == null
            ? cb.conjunction()
            : cb.equal(root.get("isGenerated"), isGenerated);
    }

    public static Specification<QuoteDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<QuoteDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<QuoteDocument> byCurrentlyValid(LocalDateTime date) {
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

    // ========================
    // DOCUMENT TYPE GROUPS
    // ========================

    public static Specification<QuoteDocument> byPrimaryDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.QUOTE_PDF),
            cb.equal(root.get("documentType"), DocumentType.PROPOSAL),
            cb.equal(root.get("documentType"), DocumentType.CONTRACT)
        );
    }

    public static Specification<QuoteDocument> byPaymentDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.INVOICE),
            cb.equal(root.get("documentType"), DocumentType.RECEIPT),
            cb.equal(root.get("documentType"), DocumentType.PAYMENT_SCHEDULE),
            cb.equal(root.get("documentType"), DocumentType.REFUND)
        );
    }

    public static Specification<QuoteDocument> byBookingDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.CONFIRMATION),
            cb.equal(root.get("documentType"), DocumentType.VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.ITINERARY)
        );
    }

    public static Specification<QuoteDocument> byTravelDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.FLIGHT_DETAILS),
            cb.equal(root.get("documentType"), DocumentType.ACCOMMODATION_DETAILS),
            cb.equal(root.get("documentType"), DocumentType.ACTIVITY_DETAILS),
            cb.equal(root.get("documentType"), DocumentType.TRANSPORT_DETAILS)
        );
    }

    // ========================
    // QUOTE FILTERS
    // ========================

    public static Specification<QuoteDocument> byQuoteCode(String quoteCode) {
        return (root, query, cb) -> {
            if (quoteCode == null || quoteCode.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("quote").get("quoteCode")), "%" + quoteCode.toLowerCase().trim() + "%");
        };
    }

    public static Specification<QuoteDocument> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("quote").get("customer").get("id"), customerId);
    }
}
