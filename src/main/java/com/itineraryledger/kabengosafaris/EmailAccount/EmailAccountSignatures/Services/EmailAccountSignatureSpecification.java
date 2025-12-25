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
}
