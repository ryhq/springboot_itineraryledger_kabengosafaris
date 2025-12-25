package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import org.springframework.data.jpa.domain.Specification;

/**
 * EmailTemplateSpecification - Dynamic query builder for EmailTemplate filtering
 *
 * Provides reusable specifications for filtering templates by various criteria
 */
public class EmailTemplateSpecification {

    /**
     * Filter by email event ID
     */
    public static Specification<EmailTemplate> emailEventId(Long emailEventId) {
        return (root, query, cb) -> cb.equal(root.get("emailEvent").get("id"), emailEventId);
    }

    /**
     * Filter by enabled status
     */
    public static Specification<EmailTemplate> enabled(Boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    /**
     * Filter by default status
     */
    public static Specification<EmailTemplate> isDefault(Boolean isDefault) {
        return (root, query, cb) -> cb.equal(root.get("isDefault"), isDefault);
    }

    /**
     * Filter by system default status
     */
    public static Specification<EmailTemplate> isSystemDefault(Boolean isSystemDefault) {
        return (root, query, cb) -> cb.equal(root.get("isSystemDefault"), isSystemDefault);
    }

    /**
     * Filter by name (partial match)
     */
    public static Specification<EmailTemplate> nameLike(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
