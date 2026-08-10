package com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;

/**
 * JPA Specifications for ItineraryDocument filtering.
 */
public class ItineraryDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<ItineraryDocument> byItineraryId(Long itineraryId) {
        return (root, query, cb) -> itineraryId == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("id"), itineraryId);
    }

    public static Specification<ItineraryDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<ItineraryDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<ItineraryDocument> byIsGenerated(Boolean isGenerated) {
        return (root, query, cb) -> isGenerated == null
            ? cb.conjunction()
            : cb.equal(root.get("isGenerated"), isGenerated);
    }

    public static Specification<ItineraryDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ItineraryDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ItineraryDocument> byCurrentlyValid(LocalDateTime date) {
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

    public static Specification<ItineraryDocument> byQuotationDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.QUOTATION),
            cb.equal(root.get("documentType"), DocumentType.PROFORMA_INVOICE),
            cb.equal(root.get("documentType"), DocumentType.INVOICE)
        );
    }

    public static Specification<ItineraryDocument> byTravelDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.TRAVEL_PLAN),
            cb.equal(root.get("documentType"), DocumentType.FINAL_ITINERARY),
            cb.equal(root.get("documentType"), DocumentType.FLIGHT_ITINERARY)
        );
    }

    public static Specification<ItineraryDocument> byVoucherDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.ACCOMMODATION_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.ACTIVITY_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.TRANSFER_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.PARK_PERMITS)
        );
    }

    // ========================
    // ITINERARY SPECIFICATIONS
    // ========================

    public static Specification<ItineraryDocument> byItineraryName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("itinerary").get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ItineraryDocument> byItineraryCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("itinerary").get("code")), "%" + code.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ItineraryDocument> byItineraryIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("isActive"), isActive);
    }

    public static Specification<ItineraryDocument> byItineraryStatus(ItineraryStatus status) {
        return (root, query, cb) -> status == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("status"), status);
    }

    public static Specification<ItineraryDocument> byItineraryTripType(TripType tripType) {
        return (root, query, cb) -> tripType == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("tripType"), tripType);
    }

    public static Specification<ItineraryDocument> byItineraryBudgetCategory(BudgetCategory budgetCategory) {
        return (root, query, cb) -> budgetCategory == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("budgetCategory"), budgetCategory);
    }

    /**
     * Any of these types — the Type filter is multi-select, and a singular enum
     * param cannot answer "quotation or invoice".
     */
    public static Specification<ItineraryDocument> byDocumentTypes(java.util.List<DocumentType> types) {
        return (root, query, cb) -> types == null || types.isEmpty()
            ? cb.conjunction()
            : root.get("documentType").in(types);
    }

    /* Validity counters, each one clickable as a filter. */

    public static Specification<ItineraryDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), java.time.LocalDateTime.now()));
    }

    public static Specification<ItineraryDocument> expiringWithin(int days) {
        return (root, query, cb) -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            return cb.and(
                cb.isNotNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), now),
                cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(days)));
        };
    }

    public static Specification<ItineraryDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }

    public static Specification<ItineraryDocument> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    /** The one search box: title, notes and either filename. */
    public static Specification<ItineraryDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like));
        };
    }
}
