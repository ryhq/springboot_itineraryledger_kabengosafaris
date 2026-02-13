package com.itineraryledger.kabengosafaris.Hero.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

/**
 * JPA Specifications for Hero filtering
 */
public class HeroSpecification {

    // ========================
    // BASIC FILTERS
    // ========================

    public static Specification<Hero> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Hero> byPage(HeroPage page) {
        return (root, query, cb) -> page == null
            ? cb.conjunction()
            : cb.equal(root.get("page"), page);
    }

    public static Specification<Hero> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Hero> byTextAlignment(String textAlignment) {
        return (root, query, cb) -> {
            if (textAlignment == null || textAlignment.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("textAlignment")), textAlignment.toLowerCase().trim());
        };
    }

    // ========================
    // RELATIONSHIP FILTERS
    // ========================

    public static Specification<Hero> byCreatedById(Long createdById) {
        return (root, query, cb) -> createdById == null
            ? cb.conjunction()
            : cb.equal(root.get("createdBy").get("id"), createdById);
    }

    public static Specification<Hero> byUpdatedById(Long updatedById) {
        return (root, query, cb) -> updatedById == null
            ? cb.conjunction()
            : cb.equal(root.get("updatedBy").get("id"), updatedById);
    }

    // ========================
    // ORDERING
    // ========================

    public static Specification<Hero> orderByDisplayOrder() {
        return (root, query, cb) -> {
            if (query != null) {
                query.orderBy(cb.asc(root.get("displayOrder")));
            }
            return cb.conjunction();
        };
    }
}
