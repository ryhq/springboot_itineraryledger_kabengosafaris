package com.itineraryledger.kabengosafaris.Blog.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;

/** Predicates for the blog image list, mirroring the other image modules. */
public class BlogImageSpecification {

    private BlogImageSpecification() {}

    public static Specification<BlogImage> byBlogId(Long blogId) {
        return (root, query, cb) -> blogId == null
            ? cb.conjunction()
            : cb.equal(root.get("blog").get("id"), blogId);
    }

    public static Specification<BlogImage> byIsPrimary(Boolean isPrimary) {
        return (root, query, cb) -> isPrimary == null
            ? cb.conjunction()
            : cb.equal(root.get("isPrimary"), isPrimary);
    }

    public static Specification<BlogImage> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<BlogImage> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String needle = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("altText")), needle),
                cb.like(cb.lower(root.get("caption")), needle),
                cb.like(cb.lower(root.get("originalFileName")), needle),
                cb.like(cb.lower(root.get("blog").get("title")), needle)
            );
        };
    }

    public static Specification<BlogImage> missingAlt() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("altText")),
            cb.equal(cb.trim(root.get("altText")), "")
        );
    }

    public static Specification<BlogImage> missingCaption() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("caption")),
            cb.equal(cb.trim(root.get("caption")), "")
        );
    }
}
