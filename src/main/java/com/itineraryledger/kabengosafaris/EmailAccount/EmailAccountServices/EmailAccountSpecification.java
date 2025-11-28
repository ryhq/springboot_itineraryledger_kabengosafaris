package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;

/**
 * EmailAccountSpecification - Provides reusable Specification objects for filtering EmailAccount entities
 *
 * Specification allows for dynamic, type-safe query construction using the Criteria API
 * Each method returns a Specification<EmailAccount> that can be combined with other specifications
 */
public class EmailAccountSpecification {

    /**
     * Filter by email address (case-insensitive)
     */
    public static Specification<EmailAccount> emailLike(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isEmpty()) {
                return cb.conjunction(); // No filter applied
            }
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    /**
     * Filter by account name (case-insensitive)
     */
    public static Specification<EmailAccount> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by enabled status
     */
    public static Specification<EmailAccount> isEnabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("enabled"), enabled);
        };
    }

    /**
     * Filter by default account status
     */
    public static Specification<EmailAccount> isDefault(Boolean isDefault) {
        return (root, query, cb) -> {
            if (isDefault == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isDefault"), isDefault);
        };
    }

    /**
     * Filter by provider type
     */
    public static Specification<EmailAccount> providerType(EmailAccountProvider providerType) {
        return (root, query, cb) -> {
            if (providerType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("providerType"), providerType);
        };
    }

    /**
     * Filter by SMTP host (case-insensitive)
     */
    public static Specification<EmailAccount> smtpHostLike(String smtpHost) {
        return (root, query, cb) -> {
            if (smtpHost == null || smtpHost.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("smtpHost")), "%" + smtpHost.toLowerCase() + "%");
        };
    }

    /**
     * Filter by SMTP port
     */
    public static Specification<EmailAccount> smtpPort(Integer smtpPort) {
        return (root, query, cb) -> {
            if (smtpPort == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("smtpPort"), smtpPort);
        };
    }

    /**
     * Filter accounts created after a specific date
     */
    public static Specification<EmailAccount> createdAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> {
            if (dateTime == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), dateTime);
        };
    }

    /**
     * Filter accounts created before a specific date
     */
    public static Specification<EmailAccount> createdBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> {
            if (dateTime == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), dateTime);
        };
    }

    /**
     * Filter accounts updated after a specific date
     */
    public static Specification<EmailAccount> updatedAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> {
            if (dateTime == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("updatedAt"), dateTime);
        };
    }

    /**
     * Filter accounts updated before a specific date
     */
    public static Specification<EmailAccount> updatedBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> {
            if (dateTime == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("updatedAt"), dateTime);
        };
    }

    /**
     * Filter by creation user (case-insensitive)
     */
    public static Specification<EmailAccount> createdByLike(String createdBy) {
        return (root, query, cb) -> {
            if (createdBy == null || createdBy.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%");
        };
    }

    /**
     * Filter by update user (case-insensitive)
     */
    public static Specification<EmailAccount> updatedByLike(String updatedBy) {
        return (root, query, cb) -> {
            if (updatedBy == null || updatedBy.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("updatedBy")), "%" + updatedBy.toLowerCase() + "%");
        };
    }

    /**
     * Filter accounts that have been tested (lastTestedAt is not null)
     */
    public static Specification<EmailAccount> hasBeeenTested() {
        return (root, query, cb) -> cb.isNotNull(root.get("lastTestedAt"));
    }

    /**
     * Filter accounts that have errors (lastErrorMessage is not null)
     */
    public static Specification<EmailAccount> hasErrors() {
        return (root, query, cb) -> cb.isNotNull(root.get("lastErrorMessage"));
    }

    /**
     * Filter accounts with minimum emails sent
     */
    public static Specification<EmailAccount> emailsSentGreaterThan(Long count) {
        return (root, query, cb) -> {
            if (count == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("emailsSentCount"), count);
        };
    }

    /**
     * Filter accounts with minimum failed emails
     */
    public static Specification<EmailAccount> emailsFailedGreaterThan(Long count) {
        return (root, query, cb) -> {
            if (count == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("emailsFailedCount"), count);
        };
    }
}
