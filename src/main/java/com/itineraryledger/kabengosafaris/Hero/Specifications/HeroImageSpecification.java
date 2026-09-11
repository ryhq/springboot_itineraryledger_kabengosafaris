package com.itineraryledger.kabengosafaris.Hero.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

/**
 * JPA Specifications for HeroImage filtering.
 */
public class HeroImageSpecification {

    // ========================
    // IMAGE SPECIFICATIONS
    // ========================

    public static Specification<HeroImage> byHeroId(Long heroId) {
        return (root, query, cb) -> heroId == null
            ? cb.conjunction()
            : cb.equal(root.get("hero").get("id"), heroId);
    }

    public static Specification<HeroImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<HeroImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    // ========================
    // HERO SPECIFICATIONS
    // ========================

    public static Specification<HeroImage> byHeroTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("hero").get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<HeroImage> byHeroPage(HeroPage page) {
        return (root, query, cb) -> page == null
            ? cb.conjunction()
            : cb.equal(root.get("hero").get("page"), page);
    }

    /**
     * The free-text search, over what the images listing shows.
     *
     * <p>The page has a search box and this module had no keyword parameter, so the box sent one
     * nothing read and every image came back. A filter that does nothing reads as a wrong answer.
     */
    public static Specification<HeroImage> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            var hero = root.join("hero", jakarta.persistence.criteria.JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
            }
            return cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("fileName"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("originalFileName"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("altText"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("caption"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like),
                cb.like(cb.lower(cb.coalesce(hero.get("title"), "")), like)
            );
        };
    }

}
