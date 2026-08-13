package com.itineraryledger.kabengosafaris.EmailEvent.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;

import jakarta.persistence.criteria.Subquery;

/** Filters for the things this system sends mail about. */
public class EmailEventSpecification {

    public static Specification<EmailEvent> isEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null
            ? cb.conjunction()
            : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<EmailEvent> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%");
    }

    /**
     * The one search box: the name and the description.
     *
     * Names are shouty constants (SEND_INVOICE, USER_REGISTRATION), so somebody typing
     * "invoice" or "welcome" has to hit the description too — that is where the words a
     * person would actually use are written.
     */
    public static Specification<EmailEvent> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            String underscored = "%" + keyword.toLowerCase().trim().replace(' ', '_') + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), underscored),
                cb.like(cb.lower(root.get("description")), like));
        };
    }

    /**
     * No template at all.
     *
     * The event fires, the system looks for something to send and finds nothing. Worth a
     * counter of its own because an enabled event with no template looks healthy from
     * every other angle.
     */
    public static Specification<EmailEvent> hasNoTemplates() {
        return (root, query, cb) -> cb.not(cb.exists(templateSubquery(root, query, cb, null)));
    }

    /** No enabled template — the same silence, arrived at a different way. */
    public static Specification<EmailEvent> hasNoEnabledTemplate() {
        return (root, query, cb) -> cb.not(cb.exists(templateSubquery(root, query, cb, "enabled")));
    }

    /** Nothing to fall back to if the custom template is deleted. */
    public static Specification<EmailEvent> hasNoSystemDefault() {
        return (root, query, cb) ->
            cb.not(cb.exists(templateSubquery(root, query, cb, "isSystemDefault")));
    }

    /**
     * Templates belonging to this event, optionally narrowed by one boolean flag.
     *
     * A subquery rather than a join: joining would multiply the event row by its templates
     * and quietly break both the counts and the paging.
     */
    private static Subquery<Long> templateSubquery(
        jakarta.persistence.criteria.Root<EmailEvent> root,
        jakarta.persistence.criteria.CriteriaQuery<?> query,
        jakarta.persistence.criteria.CriteriaBuilder cb,
        String flag
    ) {
        Subquery<Long> sub = query.subquery(Long.class);
        var template = sub.from(EmailTemplate.class);
        sub.select(cb.literal(1L));
        var predicate = cb.equal(template.get("emailEvent").get("id"), root.get("id"));
        sub.where(flag == null ? predicate : cb.and(predicate, cb.isTrue(template.get(flag))));
        return sub;
    }

    public static Specification<EmailEvent> createdAfter(LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
