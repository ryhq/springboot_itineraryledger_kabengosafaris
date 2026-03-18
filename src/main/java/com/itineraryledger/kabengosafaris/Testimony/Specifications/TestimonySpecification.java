package com.itineraryledger.kabengosafaris.Testimony.Specifications;

import java.time.LocalDateTime;

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
