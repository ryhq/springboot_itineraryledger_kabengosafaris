package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;

/**
 * EmailSignatureSpecification - Dynamic query builder for EmailSignature filtering
 *
 * Provides reusable specifications for filtering signatures by various criteria
 */
public class EmailAccountSignatureSpecification {

    /**
     * Filter by email account ID
     */
    public static Specification<EmailAccountSignature> emailAccountId(Long emailAccountId) {
        return (root, query, cb) -> cb.equal(root.get("emailAccount").get("id"), emailAccountId);
    }

    /**
     * Filter by enabled status
     */
    public static Specification<EmailAccountSignature> enabled(Boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    /**
     * Filter by default status
     */
    public static Specification<EmailAccountSignature> isDefault(Boolean isDefault) {
        return (root, query, cb) -> cb.equal(root.get("isDefault"), isDefault);
    }

    /**
     * Filter by name (partial match)
     */
    public static Specification<EmailAccountSignature> nameLike(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * The free-text search, over what the signatures listing shows.
     *
     * <p>The page has a search box and this module had no keyword parameter, so the box sent one
     * nothing read and every signature came back.
     */
    public static Specification<EmailAccountSignature> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("fileName")), like)
            );
        };
    }

}
