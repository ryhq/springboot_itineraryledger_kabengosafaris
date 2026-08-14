package com.itineraryledger.kabengosafaris.Testimony.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

public class TestimonySpecification {

    public static Specification<Testimony> byAuthorName(String authorName) {
        return (root, query, cb) -> {
            if (authorName == null || authorName.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("authorName")), "%" + authorName.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Testimony> bySource(TestimonySource source) {
        return (root, query, cb) -> source == null ? cb.conjunction() : cb.equal(root.get("source"), source);
    }

    public static Specification<Testimony> byRating(Integer rating) {
        return (root, query, cb) -> rating == null ? cb.conjunction() : cb.equal(root.get("rating"), rating);
    }

    public static Specification<Testimony> byMinRating(Integer minRating) {
        return (root, query, cb) -> minRating == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Testimony> byMaxRating(Integer maxRating) {
        return (root, query, cb) -> maxRating == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("rating"), maxRating);
    }

    public static Specification<Testimony> isApproved(Boolean approved) {
        return (root, query, cb) -> approved == null ? cb.conjunction() : cb.equal(root.get("isApproved"), approved);
    }

    public static Specification<Testimony> isFeatured(Boolean featured) {
        return (root, query, cb) -> featured == null ? cb.conjunction() : cb.equal(root.get("isFeatured"), featured);
    }

    public static Specification<Testimony> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? cb.conjunction() : cb.equal(root.get("isActive"), active);
    }

    public static Specification<Testimony> isVerifiedBooking(Boolean verified) {
        return (root, query, cb) -> verified == null ? cb.conjunction() : cb.equal(root.get("isVerifiedBooking"), verified);
    }

    // ========================
    // MULTI-VALUE FACETS (OR inside a dimension, AND across dimensions)
    // ========================

    /** Any of these sources. An empty list is no constraint, never "nothing". */
    public static Specification<Testimony> sourceIn(List<TestimonySource> sources) {
        return (root, query, cb) -> sources == null || sources.isEmpty()
            ? cb.conjunction()
            : root.get("source").in(sources);
    }

    public static Specification<Testimony> ratingIn(List<Integer> ratings) {
        return (root, query, cb) -> ratings == null || ratings.isEmpty()
            ? cb.conjunction()
            : root.get("rating").in(ratings);
    }

    /**
     * A review nobody has replied to.
     *
     * The unanswered one-star review is the one a prospective customer reads, so this is a
     * work queue rather than a statistic.
     */
    public static Specification<Testimony> hasNoAdminResponse() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("adminResponse")),
            cb.equal(cb.trim(root.get("adminResponse")), ""));
    }

    /** Praise that never made it onto the site — approval pending at 4 stars or better. */
    public static Specification<Testimony> isUnpublishedPraise() {
        return (root, query, cb) -> cb.and(
            cb.greaterThanOrEqualTo(root.get("rating"), 4),
            cb.or(cb.isNull(root.get("isApproved")), cb.isFalse(root.get("isApproved"))));
    }

    /** Complaints, by the usual reading of a five-point scale. */
    public static Specification<Testimony> isCritical() {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("rating"), 2);
    }

    public static Specification<Testimony> hasAdminResponse() {
        return (root, query, cb) -> cb.isNotNull(root.get("adminResponse"));
    }

    public static Specification<Testimony> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null ? cb.conjunction() : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Testimony> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null ? cb.conjunction() : cb.equal(root.get("safari").get("id"), safariId);
    }

    public static Specification<Testimony> bySentimentTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("sentimentTags")), "%" + tag.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Testimony> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            // Note: message is @Lob — LOWER() on CLOB types causes errors in Hibernate 6
            return cb.or(
                cb.like(cb.lower(root.get("authorName")), pattern),
                cb.like(cb.lower(root.get("sentimentTags")), pattern)
            );
        };
    }

    public static Specification<Testimony> createdAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<Testimony> createdBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), dateTime);
    }
}
