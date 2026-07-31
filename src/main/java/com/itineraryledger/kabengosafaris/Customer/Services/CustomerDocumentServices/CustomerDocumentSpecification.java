package com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;

/**
 * JPA Specifications for CustomerDocument filtering.
 */
public class CustomerDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<CustomerDocument> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<CustomerDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<CustomerDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<CustomerDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<CustomerDocument> byDocumentNumber(String documentNumber) {
        return (root, query, cb) -> {
            if (documentNumber == null || documentNumber.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("documentNumber")), "%" + documentNumber.toLowerCase().trim() + "%");
        };
    }

    public static Specification<CustomerDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<CustomerDocument> byCurrentlyValid(LocalDateTime date) {
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

    public static Specification<CustomerDocument> byIdentityDocument() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.PASSPORT),
            cb.equal(root.get("documentType"), DocumentType.ID_CARD),
            cb.equal(root.get("documentType"), DocumentType.DRIVERS_LICENSE)
        );
    }

    public static Specification<CustomerDocument> byTravelDocument() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.PASSPORT),
            cb.equal(root.get("documentType"), DocumentType.VISA),
            cb.equal(root.get("documentType"), DocumentType.INSURANCE),
            cb.equal(root.get("documentType"), DocumentType.VACCINATION)
        );
    }

    // ========================
    // CUSTOMER SPECIFICATIONS
    // ========================

    public static Specification<CustomerDocument> byCustomerName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            String lowerName = "%" + name.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("customer").get("firstName")), lowerName),
                cb.like(cb.lower(root.get("customer").get("lastName")), lowerName),
                cb.like(cb.lower(cb.concat(cb.concat(root.get("customer").get("firstName"), " "), root.get("customer").get("lastName"))), lowerName)
            );
        };
    }

    public static Specification<CustomerDocument> byCustomerType(CustomerType customerType) {
        return (root, query, cb) -> customerType == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("customerType"), customerType);
    }

    public static Specification<CustomerDocument> byCustomerEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) {
                return cb.conjunction();
            }
            // primaryEmail is a @Transient helper — query the customer's email rows instead
            query.distinct(true);
            return cb.like(
                cb.lower(root.join("customer").join("emails", jakarta.persistence.criteria.JoinType.LEFT).get("email")),
                "%" + email.toLowerCase().trim() + "%"
            );
        };
    }

    /* ---- stat-card support: every counter below is also a filter ---- */

    public static Specification<CustomerDocument> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    public static Specification<CustomerDocument> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    public static Specification<CustomerDocument> documentTypeIn(java.util.List<CustomerDocument.DocumentType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("documentType").in(types);
        };
    }

    /** Past its valid-to date. */
    public static Specification<CustomerDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), java.time.LocalDateTime.now())
        );
    }

    /** Expires within the next N days — the warning that matters before departure. */
    public static Specification<CustomerDocument> expiringWithin(int days) {
        return (root, query, cb) -> {
            var now = java.time.LocalDateTime.now();
            return cb.and(
                cb.isNotNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), now),
                cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(days))
            );
        };
    }

    public static Specification<CustomerDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }
}
