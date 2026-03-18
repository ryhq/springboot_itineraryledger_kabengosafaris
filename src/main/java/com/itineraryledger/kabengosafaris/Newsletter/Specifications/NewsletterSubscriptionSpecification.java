package com.itineraryledger.kabengosafaris.Newsletter.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;

public class NewsletterSubscriptionSpecification {

    public static Specification<NewsletterSubscription> byStatus(SubscriptionStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<NewsletterSubscription> byEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase().trim() + "%");
        };
    }

    public static Specification<NewsletterSubscription> byName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase().trim() + "%");
        };
    }

    public static Specification<NewsletterSubscription> bySource(String source) {
        return (root, query, cb) -> {
            if (source == null || source.trim().isEmpty()) return cb.conjunction();
            return cb.equal(root.get("source"), source.trim());
        };
    }

    public static Specification<NewsletterSubscription> subscribedAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("subscribedAt"), dateTime);
    }

    public static Specification<NewsletterSubscription> subscribedBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("subscribedAt"), dateTime);
    }

    public static Specification<NewsletterSubscription> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }
}
