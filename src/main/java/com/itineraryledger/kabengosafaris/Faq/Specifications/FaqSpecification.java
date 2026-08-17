package com.itineraryledger.kabengosafaris.Faq.Specifications;

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
}
