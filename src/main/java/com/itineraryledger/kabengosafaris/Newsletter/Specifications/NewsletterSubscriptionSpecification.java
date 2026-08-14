package com.itineraryledger.kabengosafaris.Newsletter.Specifications;

import java.time.LocalDateTime;
import java.util.List;

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

    /** Any of these statuses. An empty list is no constraint, never "nothing". */
    public static Specification<NewsletterSubscription> statusIn(List<SubscriptionStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    public static Specification<NewsletterSubscription> sourceIn(List<String> sources) {
        return (root, query, cb) -> {
            if (sources == null || sources.isEmpty()) return cb.conjunction();
            return cb.lower(root.get("source")).in(
                sources.stream().filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase().trim()).toList());
        };
    }

    /** Signed up, but not anybody we have as a customer. */
    public static Specification<NewsletterSubscription> hasNoCustomer() {
        return (root, query, cb) -> cb.isNull(root.get("customer"));
    }

    /** Nothing but an address — a name makes the mail-out address somebody. */
    public static Specification<NewsletterSubscription> hasNoName() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("name")),
            cb.equal(cb.trim(root.get("name")), ""));
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
