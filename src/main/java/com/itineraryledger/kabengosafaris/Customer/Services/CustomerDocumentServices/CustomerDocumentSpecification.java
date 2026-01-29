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
            return cb.like(cb.lower(root.get("customer").get("primaryEmail")), "%" + email.toLowerCase().trim() + "%");
        };
    }
}
