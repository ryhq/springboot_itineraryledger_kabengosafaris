package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import org.springframework.data.jpa.domain.Specification;

/**
 * CustomerEmailSpecification - Provides reusable Specification objects for filtering CustomerEmail entities
 */
public class CustomerEmailSpecification {

    /**
     * Filter by customer ID
     */
    public static Specification<CustomerEmail> hasCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    /**
     * Filter by email address (case-insensitive partial match)
     */
    public static Specification<CustomerEmail> emailLike(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    /**
     * Filter by email type
     */
    public static Specification<CustomerEmail> hasEmailType(EmailType emailType) {
        return (root, query, cb) -> {
            if (emailType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("emailType"), emailType);
        };
    }

    /**
     * Filter by primary status
     */
    public static Specification<CustomerEmail> isPrimary(Boolean isPrimary) {
        return (root, query, cb) -> {
            if (isPrimary == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrimary"), isPrimary);
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<CustomerEmail> isActive(Boolean isActive) {
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
    public static Specification<CustomerEmail> labelLike(String label) {
        return (root, query, cb) -> {
            if (label == null || label.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("label")), "%" + label.toLowerCase() + "%");
        };
    }

    /**
     * Search across email and label fields
     */
    public static Specification<CustomerEmail> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), likePattern),
                cb.like(cb.lower(root.get("label")), likePattern)
            );
        };
    }
}
