package com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import org.springframework.data.jpa.domain.Specification;

/**
 * CustomerPhoneSpecification - Provides reusable Specification objects for filtering CustomerPhone entities
 */
public class CustomerPhoneSpecification {

    /**
     * Filter by customer ID
     */
    public static Specification<CustomerPhone> hasCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    /**
     * Filter by phone number (case-insensitive partial match)
     */
    public static Specification<CustomerPhone> phoneNumberLike(String phoneNumber) {
        return (root, query, cb) -> {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("phoneNumber")), "%" + phoneNumber.toLowerCase() + "%");
        };
    }

    /**
     * Filter by phone type
     */
    public static Specification<CustomerPhone> hasPhoneType(PhoneType phoneType) {
        return (root, query, cb) -> {
            if (phoneType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("phoneType"), phoneType);
        };
    }

    /**
     * Filter by primary status
     */
    public static Specification<CustomerPhone> isPrimary(Boolean isPrimary) {
        return (root, query, cb) -> {
            if (isPrimary == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrimary"), isPrimary);
        };
    }

    /**
     * Filter by WhatsApp status
     */
    public static Specification<CustomerPhone> isWhatsApp(Boolean isWhatsApp) {
        return (root, query, cb) -> {
            if (isWhatsApp == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isWhatsApp"), isWhatsApp);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<CustomerPhone> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by label (case-insensitive partial match)
     */
    public static Specification<CustomerPhone> labelLike(String label) {
        return (root, query, cb) -> {
            if (label == null || label.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("label")), "%" + label.toLowerCase() + "%");
        };
    }

    /**
     * Search across phone number and label fields
     */
    public static Specification<CustomerPhone> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("phoneNumber")), likePattern),
                cb.like(cb.lower(root.get("label")), likePattern),
                cb.like(cb.lower(root.get("countryCode")), likePattern)
            );
        };
    }

    /* ---- stat-card support: every counter below is also a filter ---- */

    public static Specification<CustomerPhone> createdAfter(java.time.LocalDateTime after) {
        return (root, query, cb) ->
            after == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    public static Specification<CustomerPhone> createdBefore(java.time.LocalDateTime before) {
        return (root, query, cb) ->
            before == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    public static Specification<CustomerPhone> phoneTypeIn(java.util.List<CustomerPhone.PhoneType> types) {
        return (root, query, cb) -> {
            if (types == null || types.isEmpty()) return cb.conjunction();
            return root.get("phoneType").in(types);
        };
    }

    public static Specification<CustomerPhone> missingLabel() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("label")),
            cb.equal(cb.trim(root.get("label").as(String.class)), "")
        );
    }

    /** No country code — may not dial internationally. */
    public static Specification<CustomerPhone> missingCountryCode() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("countryCode")),
            cb.equal(cb.trim(root.get("countryCode").as(String.class)), "")
        );
    }
}
