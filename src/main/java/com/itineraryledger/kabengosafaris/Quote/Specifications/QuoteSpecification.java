package com.itineraryledger.kabengosafaris.Quote.Specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;

/**
 * JPA Specifications for Quote filtering.
 */
public class QuoteSpecification {

    // ========================
    // BASIC FILTERS
    // ========================

    public static Specification<Quote> byQuoteCode(String quoteCode) {
        return (root, query, cb) -> {
            if (quoteCode == null || quoteCode.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("quoteCode")), "%" + quoteCode.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Quote> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Quote> byStatus(QuoteStatus status) {
        return (root, query, cb) -> status == null
            ? cb.conjunction()
            : cb.equal(root.get("status"), status);
    }

    public static Specification<Quote> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Quote> byIsStoRate(Boolean isStoRate) {
        return (root, query, cb) -> isStoRate == null
            ? cb.conjunction()
            : cb.equal(root.get("isStoRate"), isStoRate);
    }

    public static Specification<Quote> byVersion(Integer version) {
        return (root, query, cb) -> version == null
            ? cb.conjunction()
            : cb.equal(root.get("version"), version);
    }

    // ========================
    // RELATIONSHIP FILTERS
    // ========================

    public static Specification<Quote> byItineraryId(Long itineraryId) {
        return (root, query, cb) -> itineraryId == null
            ? cb.conjunction()
            : cb.equal(root.get("itinerary").get("id"), itineraryId);
    }

    public static Specification<Quote> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Quote> byApproverId(Long approverId) {
        return (root, query, cb) -> approverId == null
            ? cb.conjunction()
            : cb.equal(root.get("approver").get("id"), approverId);
    }

    public static Specification<Quote> byApprovedById(Long approvedById) {
        return (root, query, cb) -> approvedById == null
            ? cb.conjunction()
            : cb.equal(root.get("approvedBy").get("id"), approvedById);
    }

    public static Specification<Quote> byCreatedById(Long createdById) {
        return (root, query, cb) -> createdById == null
            ? cb.conjunction()
            : cb.equal(root.get("createdBy").get("id"), createdById);
    }

    public static Specification<Quote> byUpdatedById(Long updatedById) {
        return (root, query, cb) -> updatedById == null
            ? cb.conjunction()
            : cb.equal(root.get("updatedBy").get("id"), updatedById);
    }

    // ========================
    // DATE FILTERS
    // ========================

    public static Specification<Quote> byValidOn(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.equal(root.get("isValid"), true),
                cb.or(
                    cb.isNull(root.get("validFrom")),
                    cb.lessThanOrEqualTo(root.get("validFrom"), date)
                ),
                cb.or(
                    cb.isNull(root.get("validTo")),
                    cb.greaterThanOrEqualTo(root.get("validTo"), date)
                )
            );
        };
    }

    public static Specification<Quote> bySentAfter(LocalDate sentAfter) {
        return (root, query, cb) -> sentAfter == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("sentDate"), sentAfter);
    }

    public static Specification<Quote> bySentBefore(LocalDate sentBefore) {
        return (root, query, cb) -> sentBefore == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("sentDate"), sentBefore);
    }

    // ========================
    // STATUS GROUPS
    // ========================

    public static Specification<Quote> byDraftStatus() {
        return (root, query, cb) -> cb.equal(root.get("status"), QuoteStatus.DRAFT);
    }

    /**
     * Filter quotes that are ready to send or waiting for customer action
     * READY: Ready to send to customer
     * SENT: Sent and awaiting customer response
     */
    public static Specification<Quote> byPendingStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.READY),
            cb.equal(root.get("status"), QuoteStatus.SENT)
        );
    }

    /**
     * Filter quotes that are in active circulation (ready to send or sent)
     * READY: Ready to send to customer
     * SENT: Sent to customer and active
     */
    public static Specification<Quote> byActiveStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.READY),
            cb.equal(root.get("status"), QuoteStatus.SENT)
        );
    }

    /**
     * Filter quotes that have reached a final state
     * ACCEPTED: Customer accepted
     * REJECTED: Customer rejected
     * EXPIRED: Quote validity period expired
     * CANCELLED: Quote cancelled
     * CONVERTED: Converted to booking/safari
     */
    public static Specification<Quote> byClosedStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.ACCEPTED),
            cb.equal(root.get("status"), QuoteStatus.REJECTED),
            cb.equal(root.get("status"), QuoteStatus.EXPIRED),
            cb.equal(root.get("status"), QuoteStatus.CANCELLED),
            cb.equal(root.get("status"), QuoteStatus.CONVERTED)
        );
    }

    /**
     * Raised since a moment — the basis of the "new this week / this month"
     * counters. Takes the cut-off rather than a day count so the caller decides
     * the window and the stats helper can reuse it.
     */
    public static Specification<Quote> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    // ========================
    // MULTI-VALUE FACETS
    // ========================

    /**
     * Any of these statuses — OR inside the dimension.
     *
     * The list page lets a stat card be shift-clicked onto another, so "Sent or
     * Accepted" is one request, not two. A single-valued enum param cannot
     * express that and would 400 on the comma.
     */
    public static Specification<Quote> byStatuses(java.util.List<QuoteStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    /** Any of the named stages (draft / pending / active / closed). */
    public static Specification<Quote> byStatusGroups(java.util.List<String> groups) {
        return (root, query, cb) -> {
            if (groups == null || groups.isEmpty()) return cb.conjunction();

            java.util.List<jakarta.persistence.criteria.Predicate> any = new java.util.ArrayList<>();
            for (String group : groups) {
                if (group == null || group.isBlank()) continue;
                Specification<Quote> spec = switch (group.trim().toLowerCase()) {
                    case "draft" -> byDraftStatus();
                    case "pending" -> byPendingStatuses();
                    case "active" -> byActiveStatuses();
                    case "closed" -> byClosedStatuses();
                    // an unknown stage narrows to nothing rather than quietly
                    // widening to everything
                    default -> (r, q, c) -> c.disjunction();
                };
                any.add(spec.toPredicate(root, query, cb));
            }
            return any.isEmpty() ? cb.conjunction() : cb.or(any.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * The one search box: code, title, description, and the names a quote is
     * actually looked up by — its customer and its itinerary.
     *
     * Joins are LEFT so a quote is never dropped for want of the thing being
     * searched, and the query is DISTINCT because a join can otherwise repeat a
     * row and corrupt both the page count and the totals.
     */
    public static Specification<Quote> byKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String needle = "%" + keyword.toLowerCase().trim() + "%";

            if (query != null) query.distinct(true);

            var customer = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            var itinerary = root.join("itinerary", jakarta.persistence.criteria.JoinType.LEFT);

            return cb.or(
                cb.like(cb.lower(root.get("quoteCode")), needle),
                cb.like(cb.lower(root.get("title")), needle),
                cb.like(cb.lower(root.get("description")), needle),
                cb.like(cb.lower(customer.get("firstName")), needle),
                cb.like(cb.lower(customer.get("lastName")), needle),
                cb.like(cb.lower(customer.get("companyName")), needle),
                cb.like(cb.lower(itinerary.get("name")), needle),
                cb.like(cb.lower(itinerary.get("code")), needle)
            );
        };
    }
}
