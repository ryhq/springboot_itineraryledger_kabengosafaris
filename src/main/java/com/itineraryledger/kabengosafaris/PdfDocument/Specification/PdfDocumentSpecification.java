package com.itineraryledger.kabengosafaris.PdfDocument.Specification;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;

import jakarta.persistence.criteria.Subquery;

/** Filters for the kinds of document this system produces. */
public class PdfDocumentSpecification {

    public static Specification<PdfDocument> isEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null
            ? cb.conjunction()
            : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<PdfDocument> nameLike(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%");
    }

    /**
     * The one search box: display name, system name and description.
     *
     * System names are shouty constants (FULL_INVOICE, PAYMENT_RECEIPT), so somebody typing
     * "invoice" or "receipt" has to hit the display name and the description too.
     */
    public static Specification<PdfDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            String underscored = "%" + keyword.toLowerCase().trim().replace(' ', '_') + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), underscored),
                cb.like(cb.lower(root.get("displayName")), like),
                cb.like(cb.lower(root.get("description")), like));
        };
    }

    /** No template at all — the document cannot be produced. */
    public static Specification<PdfDocument> hasNoTemplates() {
        return (root, query, cb) -> cb.not(cb.exists(templates(root, query, cb, null)));
    }

    /** A template exists but none is enabled: the same silence, arrived at differently. */
    public static Specification<PdfDocument> hasNoEnabledTemplate() {
        return (root, query, cb) -> cb.not(cb.exists(templates(root, query, cb, "enabled")));
    }

    /** Nothing shipped to fall back to if a customised template goes wrong. */
    public static Specification<PdfDocument> hasNoSystemDefault() {
        return (root, query, cb) -> cb.not(cb.exists(templates(root, query, cb, "isSystemDefault")));
    }

    /**
     * This document's templates, optionally narrowed by one boolean flag.
     *
     * A subquery rather than a join: joining multiplies the document row by its templates
     * and quietly breaks both the counts and the paging.
     */
    private static Subquery<Long> templates(
        jakarta.persistence.criteria.Root<PdfDocument> root,
        jakarta.persistence.criteria.CriteriaQuery<?> query,
        jakarta.persistence.criteria.CriteriaBuilder cb,
        String flag
    ) {
        Subquery<Long> sub = query.subquery(Long.class);
        var template = sub.from(PdfTemplate.class);
        sub.select(cb.literal(1L));
        var owns = cb.equal(template.get("pdfDocument").get("id"), root.get("id"));
        sub.where(flag == null ? owns : cb.and(owns, cb.isTrue(template.get(flag))));
        return sub;
    }
}
