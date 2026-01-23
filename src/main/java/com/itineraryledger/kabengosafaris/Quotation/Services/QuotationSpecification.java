package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * QuotationSpecification - Provides reusable Specification objects for filtering Quotation entities
 */
public class QuotationSpecification {

    /**
     * Filter by customer ID
     */
    public static Specification<Quotation> hasCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    /**
     * Filter by itinerary ID
     */
    public static Specification<Quotation> hasItineraryId(Long itineraryId) {
        return (root, query, cb) -> {
            if (itineraryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("itinerary").get("id"), itineraryId);
        };
    }

    /**
     * Filter by status
     */
    public static Specification<Quotation> hasStatus(QuotationStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Filter by assigned user
     */
    public static Specification<Quotation> assignedTo(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("assignedTo"), userId);
        };
    }

    /**
     * Filter by created by user
     */
    public static Specification<Quotation> createdBy(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("createdBy"), userId);
        };
    }

    /**
     * Filter by name (partial match)
     */
    public static Specification<Quotation> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by code (partial match)
     */
    public static Specification<Quotation> codeLike(String code) {
        return (root, query, cb) -> {
            if (code == null || code.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
        };
    }

    /**
     * Filter by start date range
     */
    public static Specification<Quotation> startDateBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("startDate"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("startDate"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("startDate"), endDate);
        };
    }

    /**
     * Filter by creation date range
     */
    public static Specification<Quotation> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }
            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            }
            if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }

    /**
     * Filter by valid until date
     */
    public static Specification<Quotation> validUntilAfter(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("validUntil"), date);
        };
    }

    /**
     * Filter expired quotations
     */
    public static Specification<Quotation> isExpired(Boolean expired) {
        return (root, query, cb) -> {
            if (expired == null) {
                return cb.conjunction();
            }
            LocalDate today = LocalDate.now();
            if (expired) {
                return cb.lessThan(root.get("validUntil"), today);
            }
            return cb.or(
                cb.isNull(root.get("validUntil")),
                cb.greaterThanOrEqualTo(root.get("validUntil"), today)
            );
        };
    }

    /**
     * Filter by currency
     */
    public static Specification<Quotation> hasCurrency(String currency) {
        return (root, query, cb) -> {
            if (currency == null || currency.isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("currency"), currency);
        };
    }

    /**
     * Exclude revisions (only show original quotations)
     */
    public static Specification<Quotation> isOriginal() {
        return (root, query, cb) -> cb.isNull(root.get("parentQuotation"));
    }

    /**
     * Search across multiple fields (name, code, customer name)
     */
    public static Specification<Quotation> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("customer").get("firstName")), pattern),
                cb.like(cb.lower(root.get("customer").get("lastName")), pattern),
                cb.like(cb.lower(root.get("customer").get("companyName")), pattern)
            );
        };
    }
}
