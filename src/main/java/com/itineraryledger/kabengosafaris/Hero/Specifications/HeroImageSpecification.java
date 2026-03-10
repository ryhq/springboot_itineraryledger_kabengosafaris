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
}
