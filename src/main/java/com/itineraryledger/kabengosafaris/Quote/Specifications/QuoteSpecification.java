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

    public static Specification<Quote> byPendingStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.PENDING_REVIEW),
            cb.equal(root.get("status"), QuoteStatus.CUSTOMER_REVIEWING),
            cb.equal(root.get("status"), QuoteStatus.CUSTOMER_REQUESTED_CHANGES)
        );
    }

    public static Specification<Quote> byActiveStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.SENT),
            cb.equal(root.get("status"), QuoteStatus.CUSTOMER_REVIEWING),
            cb.equal(root.get("status"), QuoteStatus.REVISED)
        );
    }

    public static Specification<Quote> byClosedStatuses() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("status"), QuoteStatus.ACCEPTED),
            cb.equal(root.get("status"), QuoteStatus.REJECTED),
            cb.equal(root.get("status"), QuoteStatus.EXPIRED),
            cb.equal(root.get("status"), QuoteStatus.CANCELLED),
            cb.equal(root.get("status"), QuoteStatus.CONVERTED)
        );
    }
}
