package com.itineraryledger.kabengosafaris.Blog.Specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Blog.Entity.Blog;

import jakarta.persistence.criteria.Predicate;

/**
 * The blog list's predicates.
 *
 * Every text column here is TEXT rather than a CLOB, which is what makes LOWER(...) legal:
 * a {@code @Lob} field throws at query time in Hibernate 6, and this search reads the body.
 */
public class BlogSpecification {

    private BlogSpecification() {}

    public static Specification<Blog> titleLike(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Blog> authorLike(String author) {
        return (root, query, cb) -> author == null || author.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    public static Specification<Blog> isPublished(Boolean published) {
        return (root, query, cb) -> published == null
            ? cb.conjunction()
            : cb.equal(root.get("isPublished"), published);
    }

    /** One tag, matched exactly, through the element collection. */
    public static Specification<Blog> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return cb.conjunction();
            query.distinct(true);
            jakarta.persistence.criteria.Expression<String> tags =
                root.<java.util.List<String>, String>joinList("tags", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.equal(cb.lower(tags), tag.toLowerCase());
        };
    }

    /**
     * The one search box: what the article is called, where it lives, who wrote it, what it
     * is about — and the body, because "which post mentions Ndutu" is the question people
     * actually arrive with.
     */
    public static Specification<Blog> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String needle = "%" + keyword.toLowerCase().trim() + "%";
            query.distinct(true);
            jakarta.persistence.criteria.Expression<String> tags =
                root.<java.util.List<String>, String>joinList("tags", jakarta.persistence.criteria.JoinType.LEFT);
            Predicate[] any = {
                cb.like(cb.lower(root.get("title")), needle),
                cb.like(cb.lower(root.get("slug")), needle),
                cb.like(cb.lower(root.get("excerpt")), needle),
                cb.like(cb.lower(root.get("author")), needle),
                cb.like(cb.lower(root.get("bodyJson")), needle),
                cb.like(cb.lower(tags), needle),
            };
            return cb.or(any);
        };
    }

    public static Specification<Blog> publishedAfter(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("publishDate"), date);
    }

    public static Specification<Blog> publishedBefore(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("publishDate"), date);
    }

    /* ---- data quality: every one of these is something somebody can go and fix ---- */

    public static Specification<Blog> missingExcerpt() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("excerpt")),
            cb.equal(cb.trim(root.get("excerpt")), "")
        );
    }

    public static Specification<Blog> missingCover() {
        return (root, query, cb) -> {
            var sub = query.subquery(Long.class);
            var image = sub.from(com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage.class);
            sub.select(cb.literal(1L)).where(
                cb.equal(image.get("blog").get("id"), root.get("id")),
                cb.isTrue(image.get("isPrimary"))
            );
            return cb.not(cb.exists(sub));
        };
    }

    public static Specification<Blog> missingMeta() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("metaDescription")),
            cb.equal(cb.trim(root.get("metaDescription")), "")
        );
    }

    public static Specification<Blog> emptyBody() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("bodyJson")),
            cb.equal(cb.trim(root.get("bodyJson")), ""),
            cb.equal(cb.trim(root.get("bodyJson")), "[]")
        );
    }
}
