package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest;

/**
 * The predicates the chase list is built from.
 *
 * Every one of these is reachable as a filter and every filter has a counter, which is the rule for
 * listing pages here: a figure nobody can click is decoration, and a filter with no figure hides how
 * much it is hiding.
 */
public final class AvailabilityRequestSpecification {

    private AvailabilityRequestSpecification() {}

    public static Specification<AvailabilityRequest> statusIn(List<AvailabilityRequest.Status> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * Asked, nothing back, and past its chase date.
     *
     * Computed from the stored date rather than a flag, so it becomes true on its own as the clock
     * passes. A stored "isChaseDue" would need a job to keep it honest and would be wrong until it ran.
     */
    public static Specification<AvailabilityRequest> chaseDue(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), AvailabilityRequest.Status.SENT),
            cb.isNotNull(root.get("chaseDueAt")),
            cb.lessThanOrEqualTo(root.get("chaseDueAt"), now));
    }

    /** Asked, nothing back, and not yet due — the healthy waiting state. */
    public static Specification<AvailabilityRequest> awaiting(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), AvailabilityRequest.Status.SENT),
            cb.or(
                cb.isNull(root.get("chaseDueAt")),
                cb.greaterThan(root.get("chaseDueAt"), now)));
    }

    /** They answered and nobody has decided what it means yet — the real work of a morning. */
    public static Specification<AvailabilityRequest> repliedUndecided() {
        return (root, query, cb) -> cb.equal(root.get("status"), AvailabilityRequest.Status.REPLIED);
    }

    public static Specification<AvailabilityRequest> closedFor(AvailabilityRequest.ClosedReason reason) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), AvailabilityRequest.Status.CLOSED),
            cb.equal(root.get("closedReason"), reason));
    }

    public static Specification<AvailabilityRequest> forSafari(Long safariId) {
        if (safariId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("safari").get("id"), safariId);
    }

    public static Specification<AvailabilityRequest> forAccommodation(Long accommodationId) {
        if (accommodationId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("accommodation").get("id"), accommodationId);
    }

    public static Specification<AvailabilityRequest> sentAfter(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sentAt"), from);
    }

    public static Specification<AvailabilityRequest> sentBefore(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("sentAt"), to);
    }

    /**
     * Free text over the property, the safari and the subject.
     *
     * Those are the three things somebody remembers: the camp they wrote to, the trip it was for, or
     * a phrase from the subject line.
     */
    public static Specification<AvailabilityRequest> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            return cb.or(
                cb.like(cb.lower(root.get("accommodation").get("name")), like),
                cb.like(cb.lower(root.get("safari").get("name")), like),
                cb.like(cb.lower(root.get("safari").get("code")), like),
                cb.like(cb.lower(root.get("subject")), like),
                cb.like(cb.lower(root.get("toAddress")), like));
        };
    }
}
