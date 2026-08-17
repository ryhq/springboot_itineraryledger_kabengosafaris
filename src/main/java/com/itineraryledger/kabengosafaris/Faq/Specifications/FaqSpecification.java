package com.itineraryledger.kabengosafaris.Faq.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Faq.Entity.Faq;

/** Predicates for the FAQ list. */
public class FaqSpecification {

    private FaqSpecification() {}

    public static Specification<Faq> isActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Faq> byCategory(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
            ? cb.conjunction()
            : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    /** Both sides of the pair: people search for the answer's words as often as the question's. */
    public static Specification<Faq> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String needle = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("question")), needle),
                cb.like(cb.lower(root.get("answer")), needle),
                cb.like(cb.lower(root.get("category")), needle)
            );
        };
    }

    public static Specification<Faq> missingCategory() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("category")),
            cb.equal(cb.trim(root.get("category")), "")
        );
    }

    public static Specification<Faq> hasCategory() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("category")),
            cb.notEqual(cb.trim(root.get("category")), "")
        );
    }

    /**
     * An answer too short to be an answer.
     *
     * Not a style rule — a one-line reply to "do I need vaccinations?" is the kind of thing
     * that reads as evasive on a public page, and it is worth being able to list them.
     */
    public static Specification<Faq> thinAnswer(int minChars) {
        return (root, query, cb) -> cb.lessThan(cb.length(cb.coalesce(root.get("answer"), "")), minChars);
    }

    public static Specification<Faq> createdAfter(LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    public static Specification<Faq> updatedAfter(LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("updatedAt"), since);
    }

    /** Untouched for a long while — worth re-reading before a season starts. */
    public static Specification<Faq> notUpdatedSince(LocalDateTime before) {
        return (root, query, cb) -> before == null
            ? cb.conjunction()
            : cb.lessThan(root.get("updatedAt"), before);
    }
}
