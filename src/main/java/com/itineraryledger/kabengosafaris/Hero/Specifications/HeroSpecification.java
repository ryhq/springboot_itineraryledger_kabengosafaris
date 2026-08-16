package com.itineraryledger.kabengosafaris.Hero.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import jakarta.persistence.criteria.Subquery;

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
    // MULTI-VALUE FACETS (OR inside a dimension, AND across dimensions)
    // ========================

    /** Any of these pages. An empty list is no constraint, never "nothing". */
    public static Specification<Hero> pageIn(List<HeroPage> pages) {
        return (root, query, cb) -> pages == null || pages.isEmpty()
            ? cb.conjunction()
            : root.get("page").in(pages);
    }

    public static Specification<Hero> textAlignmentIn(List<String> alignments) {
        return (root, query, cb) -> {
            if (alignments == null || alignments.isEmpty()) return cb.conjunction();
            return cb.lower(root.get("textAlignment")).in(
                alignments.stream().filter(a -> a != null && !a.isBlank())
                    .map(a -> a.toLowerCase().trim()).toList());
        };
    }

    /**
     * The one search box: the words a banner actually shows.
     *
     * Headline, subtitle, body and the button's own text — somebody hunting for the banner
     * that says "Book your safari" is as likely to remember the button as the heading, and
     * as likely to remember a phrase from the paragraph as either.
     *
     * The body used to be excluded because @Lob made LOWER() over it throw. The annotation
     * was wrong rather than the query, so it went instead.
     */
    public static Specification<Hero> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("subtitle")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("ctaText")), like));
        };
    }

    // ========================
    // WORTH CHECKING — each of these is a card AND a filter
    // ========================

    /**
     * A banner with no image behind it.
     *
     * A hero IS its picture; one without renders as a coloured block on the live site,
     * which is the kind of thing nobody notices until a customer mentions it.
     *
     * A subquery, not a join: joining multiplies the hero row by its images and quietly
     * breaks both the counts and the paging.
     */
    public static Specification<Hero> hasNoImages() {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var image = sub.from(HeroImage.class);
            sub.select(cb.literal(1L));
            sub.where(cb.equal(image.get("hero").get("id"), root.get("id")));
            return cb.not(cb.exists(sub));
        };
    }

    /** A call to action with nowhere to go — a button that does nothing when clicked. */
    public static Specification<Hero> hasBrokenCta() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("ctaText")),
            cb.notEqual(cb.trim(root.get("ctaText")), ""),
            cb.or(cb.isNull(root.get("ctaLink")), cb.equal(cb.trim(root.get("ctaLink")), "")));
    }

    public static Specification<Hero> createdAfter(LocalDateTime when) {
        return (root, query, cb) -> when == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), when);
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
