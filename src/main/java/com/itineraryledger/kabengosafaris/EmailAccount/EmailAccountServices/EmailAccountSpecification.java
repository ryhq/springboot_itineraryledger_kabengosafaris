package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

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
     * Filter accounts that have errors (lastErrorMessage is not null)
     */
    public static Specification<EmailAccount> hasErrors() {
        return (root, query, cb) -> cb.isNotNull(root.get("lastErrorMessage"));
    }

    /**
     * Filter by description (case-insensitive)
     */
    public static Specification<EmailAccount> descriptionLike(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    /**
     * Filter by TLS enabled status
     */
    public static Specification<EmailAccount> useTls(Boolean useTls) {
        return (root, query, cb) -> {
            if (useTls == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("useTls"), useTls);
        };
    }

    /**
     * Filter by SSL enabled status
     */
    public static Specification<EmailAccount> useSsl(Boolean useSsl) {
        return (root, query, cb) -> {
            if (useSsl == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("useSsl"), useSsl);
        };
    }

    /**
     * Filter by error message (case-insensitive)
     */
    public static Specification<EmailAccount> errorMessageLike(String errorMessage) {
        return (root, query, cb) -> {
            if (errorMessage == null || errorMessage.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("lastErrorMessage")), "%" + errorMessage.toLowerCase() + "%");
        };
    }

    /**
     * Filter accounts by SMTP username (case-insensitive)
     */
    public static Specification<EmailAccount> smtpUsernameLike(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("smtpUsername")), "%" + username.toLowerCase() + "%");
        };
    }
}
