package com.itineraryledger.kabengosafaris.Testimony.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;

public class TestimonyImageSpecification {

    public static Specification<TestimonyImage> byTestimonyId(Long testimonyId) {
        return (root, query, cb) -> testimonyId == null ? cb.conjunction() : cb.equal(root.get("testimony").get("id"), testimonyId);
    }

    public static Specification<TestimonyImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null ? cb.conjunction() : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<TestimonyImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<TestimonyImage> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("altText")), pattern),
                cb.like(cb.lower(root.get("caption")), pattern),
                cb.like(cb.lower(root.get("description").as(String.class)), pattern)
            );
        };
    }
}
