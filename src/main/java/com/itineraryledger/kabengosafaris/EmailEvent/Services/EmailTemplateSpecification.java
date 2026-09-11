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

    /**
     * The free-text search, over what the templates listing shows.
     *
     * <p>The page has a search box and this module had no keyword parameter, so the box sent one
     * nothing read and every template came back. Joins the event, because a template is found by
     * what it is FOR: somebody types "booking" and that word is on the event, not on a row called
     * "Default".
     */
    public static Specification<EmailTemplate> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            var event = root.join("emailEvent", jakarta.persistence.criteria.JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
            }
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(event.get("name")), like),
                cb.like(cb.lower(event.get("displayName")), like)
            );
        };
    }

}
